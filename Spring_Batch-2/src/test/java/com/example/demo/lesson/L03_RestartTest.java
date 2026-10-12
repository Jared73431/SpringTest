package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 3 課：失敗之後重新執行。
 *
 * <p>
 * Job 失敗時，JobInstance 沒有完成。用<b>同一組參數</b>再執行一次，就是「重新執行（restart）」：
 * <ul>
 * <li>沿用同一個 JobInstance，新增一筆 JobExecution</li>
 * <li>已經 COMPLETED 的 Step 跳過，從失敗的 Step 繼續</li>
 * </ul>
 *
 * 所以 Step 要切成「做完就不必再做」的單位：例如先備份、再更新，更新失敗時重新執行不會再備份一次。
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class L03_RestartTest extends PostgresContainerTestBase {

	static final AtomicInteger PREPARE_RUNS = new AtomicInteger();
	static final AtomicInteger PROCESS_RUNS = new AtomicInteger();
	/** true 時第二個 Step 失敗：模擬資料庫斷線之類的暫時性錯誤 */
	static final AtomicBoolean PROCESS_FAILS = new AtomicBoolean();

	@TestConfiguration
	static class TwoStepJobConfig {
		@Bean
		Job twoStepJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			Step prepare = new StepBuilder("prepare", jobRepository).tasklet((contribution, chunkContext) -> {
				PREPARE_RUNS.incrementAndGet();
				return RepeatStatus.FINISHED;
			}, transactionManager).build();
			Step process = new StepBuilder("process", jobRepository).tasklet((contribution, chunkContext) -> {
				PROCESS_RUNS.incrementAndGet();
				if (PROCESS_FAILS.get()) {
					throw new IllegalStateException("database unavailable");
				}
				return RepeatStatus.FINISHED;
			}, transactionManager).build();
			return new JobBuilder("twoStepJob", jobRepository).start(prepare).next(process).build();
		}
	}

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job twoStepJob;

	@Autowired
	private JdbcTemplate jdbc;

	private final JobParameters october12 = new JobParametersBuilder()
			.addLocalDate("date", LocalDate.of(2026, 10, 12)).toJobParameters();

	@BeforeEach
	void cleanUp() {
		deleteBatchHistory(jdbc);
		PREPARE_RUNS.set(0);
		PROCESS_RUNS.set(0);
		PROCESS_FAILS.set(true);
	}

	@Test
	void failedJob_shouldRecordWhichStepFailed() throws Exception {
		var execution = jobOperator.start(twoStepJob, october12);

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(jdbc.queryForList("select step_name || ':' || status from batch_step_execution order by step_execution_id",
				String.class)).containsExactly("prepare:COMPLETED", "process:FAILED");
		// 失敗原因存在 exit_message，不必翻 log
		assertThat(jdbc.queryForObject("select exit_message from batch_step_execution where step_name = 'process'",
				String.class)).contains("database unavailable");
	}

	@Test
	void runningAgainWithSameParameters_shouldRestartFromTheFailedStep() throws Exception {
		jobOperator.start(twoStepJob, october12);
		PROCESS_FAILS.set(false); // 問題排除

		var restarted = jobOperator.start(twoStepJob, october12);

		assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		// 同一個 JobInstance，兩筆 JobExecution：FAILED、COMPLETED
		assertThat(jdbc.queryForObject("select count(*) from batch_job_instance", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForList("select status from batch_job_execution order by job_execution_id", String.class))
				.containsExactly("FAILED", "COMPLETED");
		// prepare 已經完成，重新執行時跳過；process 執行了兩次
		assertThat(PREPARE_RUNS).hasValue(1);
		assertThat(PROCESS_RUNS).hasValue(2);
	}
}
