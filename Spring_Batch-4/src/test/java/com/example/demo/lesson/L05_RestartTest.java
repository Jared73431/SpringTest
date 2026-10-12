package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.price.PriceUpdate;
import com.example.demo.price.PriceUpdateJobConfig;

/**
 * 第 5 課：從中斷處繼續 —— 失敗後用同一組參數再執行，只處理剩下的部分。
 *
 * <pre>
 * 價格更新檔 6 行，chunk = 2：
 * 第 1 次：chunk 1（第 1、2 行）commit → chunk 2 寫入時資料庫斷線 → FAILED
 * 第 2 次（同一個 input.file）：從第 3 行開始讀 → chunk 2、3 → COMPLETED
 * </pre>
 *
 * 為什麼知道從第 3 行開始：FlatFileItemReader 每次 commit 時把「讀了幾行」存進 Step 的 ExecutionContext
 * （BATCH_STEP_EXECUTION_CONTEXT），重新執行時先跳過這些行。
 *
 * <p>
 * 這需要用<b>業務參數</b>識別（同一個檔案 = 同一個 JobInstance）。用時間戳的話，每次都是新的 JobInstance，
 * 會從第 1 行重新開始（這個 Job 剛好是冪等的，所以結果也正確，只是做了白工；第 6 課）。
 */
@SpringBootTest
class L05_RestartTest extends PostgresContainerTestBase {

	private static final String[] ISBNS = { "9781617297571", "9781491983461", "9780134685991", "9780321356680",
			"9780132350884", "9781617293566" };

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

	private final AtomicBoolean databaseDown = new AtomicBoolean();

	@BeforeEach
	void cleanUp() throws Exception {
		resetData(dataSource);
		// 資料庫斷線時，寫入第 3 行所在的 chunk 會失敗
		doAnswer(invocation -> {
			Chunk<PriceUpdate> chunk = invocation.getArgument(0);
			if (databaseDown.get() && chunk.getItems().stream().anyMatch(item -> item.isbn().equals(ISBNS[2]))) {
				throw new DataAccessResourceFailureException("database unavailable");
			}
			return invocation.callRealMethod();
		}).when(priceUpdateWriter).write(any());
	}

	private JobParameters sixLineFile() throws Exception {
		var csv = new StringBuilder("isbn,price\n");
		for (String isbn : ISBNS) {
			csv.append(isbn).append(",1.00\n");
		}
		var file = Files.writeString(tempDir.resolve("six-lines.csv"), csv);
		return new JobParametersBuilder().addString(PriceUpdateJobConfig.INPUT_FILE, file.toUri().toString())
				.toJobParameters();
	}

	private static StepExecution priceUpdateStep(JobExecution execution) {
		return execution.getStepExecutions().stream()
				.filter(step -> step.getStepName().equals("priceUpdateStep"))
				.findFirst().orElseThrow();
	}

	private int booksAtOneDollar() {
		return jdbc.queryForObject("select count(*) from book where price = 1.00", Integer.class);
	}

	@Test
	void runningAgainWithTheSameFile_shouldContinueFromTheLastCommittedChunk() throws Exception {
		var parameters = sixLineFile();

		databaseDown.set(true);
		var first = jobOperator.start(priceUpdateJob, parameters);

		assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(priceUpdateStep(first).getCommitCount()).isEqualTo(1);
		assertThat(booksAtOneDollar()).isEqualTo(2); // 第 1 個 chunk 已經 commit

		databaseDown.set(false); // 資料庫恢復
		var second = jobOperator.start(priceUpdateJob, parameters);

		assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(second.getJobInstance().getId()).isEqualTo(first.getJobInstance().getId());
		assertThat(priceUpdateStep(second).getReadCount()).isEqualTo(4); // 從第 3 行開始，只讀剩下的 4 行
		assertThat(booksAtOneDollar()).isEqualTo(6);
	}
}
