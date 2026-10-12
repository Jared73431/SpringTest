package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.price.PriceUpdate;
import com.example.demo.price.PriceUpdateJobConfig;

/**
 * 第 4 課：Skip 與 Retry —— 不讓一行壞資料或一次暫時性錯誤，讓整個 Job 失敗。
 *
 * <pre>
 *            什麼時候用                          priceUpdateStep 的設定
 * Skip       這一筆有問題，重試也不會好            格式錯誤（FlatFileParseException）、ISBN 不存在，最多 10 筆
 * Retry      暫時性的錯誤，再試一次可能就好        TransientDataAccessException（deadlock、逾時），最多 3 次
 * 都不是     要人處理的錯誤                        Step 失敗（例如違反資料庫限制）
 * </pre>
 *
 * 使用正式的 priceUpdateJob；Writer 用 @MockitoSpyBean 包起來，模擬資料庫錯誤。
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class L04_SkipRetryTest extends PostgresContainerTestBase {

	@MockitoSpyBean
	private JdbcBatchItemWriter<PriceUpdate> priceUpdateWriter;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job priceUpdateJob;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	@TempDir
	Path tempDir;

	@BeforeEach
	void cleanUp() {
		resetData(dataSource);
	}

	private JobExecution runPriceUpdate(String inputFile) throws Exception {
		return jobOperator.start(priceUpdateJob,
				new JobParametersBuilder().addString(PriceUpdateJobConfig.INPUT_FILE, inputFile).toJobParameters());
	}

	private static StepExecution priceUpdateStep(JobExecution execution) {
		return execution.getStepExecutions().stream()
				.filter(step -> step.getStepName().equals("priceUpdateStep"))
				.findFirst().orElseThrow();
	}

	/** 只有一個 chunk 的價格更新檔（2 行） */
	private String twoLineFile() throws Exception {
		return Files.writeString(tempDir.resolve("two-lines.csv"),
				"isbn,price\n9781617297571,49.00\n9780134685991,52.50\n").toUri().toString();
	}

	@Test
	void skip_shouldSkipBadLinesAndUnknownIsbns_andLogWhy(CapturedOutput output) throws Exception {
		var execution = runPriceUpdate("classpath:price-updates/price-update-2026-10-12.csv");

		var step = priceUpdateStep(execution);
		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(step.getReadSkipCount()).isEqualTo(1); // 第 4 行價格 abc
		assertThat(step.getProcessSkipCount()).isEqualTo(1); // ISBN 9999999999999 不存在
		assertThat(step.getWriteCount()).isEqualTo(4);
		// 跳過不是忽略：PriceUpdateSkipListener 記錄每一筆跳過的資料與原因
		assertThat(output.getOut())
				.contains("Skipped line 4 [9780321356680,abc]")
				.contains("Skipped PriceUpdate[isbn=9999999999999, price=10.00]: Unknown ISBN: 9999999999999");
	}

	@Test
	void retry_shouldTryAgain_whenTheDatabaseErrorIsTransient() throws Exception {
		// 第一次寫入遇到 deadlock，之後正常
		doThrow(new TransientDataAccessResourceException("deadlock detected"))
				.doCallRealMethod()
				.when(priceUpdateWriter).write(any());

		var execution = runPriceUpdate(twoLineFile());

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		verify(priceUpdateWriter, times(2)).write(any());
		assertThat(jdbc.queryForObject("select price from book where isbn = '9781617297571'", String.class))
				.isEqualTo("49.00");
	}

	@Test
	void otherErrors_shouldFailTheStepWithoutRetrying() throws Exception {
		doThrow(new DataIntegrityViolationException("check constraint violated"))
				.when(priceUpdateWriter).write(any());

		var execution = runPriceUpdate(twoLineFile());

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
		verify(priceUpdateWriter, times(1)).write(any());
		doCallRealMethod().when(priceUpdateWriter).write(any());
	}
}
