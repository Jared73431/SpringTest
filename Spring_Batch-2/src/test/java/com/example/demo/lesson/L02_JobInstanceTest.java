package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 2 課：JobInstance 怎麼識別？
 *
 * <pre>
 * JobInstance = Job 名稱 + 識別參數（identifying JobParameters）
 * </pre>
 *
 * <ul>
 * <li>同一組識別參數已經 COMPLETED → 拒絕執行（JobInstanceAlreadyCompleteException）</li>
 * <li>識別參數不同 → 新的 JobInstance</li>
 * <li>非識別參數（identifying=false）不同 → 仍然是同一個 JobInstance</li>
 * <li>RunIdIncrementer：每次自動加一個 run.id，所以每次都是新的 JobInstance</li>
 * </ul>
 *
 * 「拒絕重複執行」是保護機制：用業務日期當識別參數，同一天的資料就不會被處理兩次。
 * 只有「每次都要重新做一次、重複執行也沒關係」的 Job 才適合 RunIdIncrementer。
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class L02_JobInstanceTest extends PostgresContainerTestBase {

	@TestConfiguration
	static class IncrementerJobConfig {
		// 和 helloJob 使用同一個 Step，只多了 incrementer
		@Bean
		Job incrementedJob(JobRepository jobRepository, Step helloStep) {
			return new JobBuilder("incrementedJob", jobRepository).incrementer(new RunIdIncrementer())
					.start(helloStep).build();
		}
	}

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job helloJob;

	@Autowired
	private Job incrementedJob;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanUp() {
		deleteBatchHistory(jdbc);
	}

	private static JobParameters date(int day, String triggeredBy) {
		return new JobParametersBuilder()
				.addLocalDate("date", LocalDate.of(2026, 10, day))
				.addString("triggeredBy", triggeredBy, false)
				.toJobParameters();
	}

	private int jobInstances() {
		return jdbc.queryForObject("select count(*) from batch_job_instance", Integer.class);
	}

	@Test
	void sameIdentifyingParameters_shouldBeRejected_whenAlreadyCompleted() throws Exception {
		jobOperator.start(helloJob, date(12, "manual"));

		assertThatThrownBy(() -> jobOperator.start(helloJob, date(12, "manual")))
				.isInstanceOf(JobInstanceAlreadyCompleteException.class);
		assertThat(jobInstances()).isEqualTo(1);
	}

	@Test
	void differentIdentifyingParameters_shouldCreateNewJobInstance() throws Exception {
		jobOperator.start(helloJob, date(12, "manual"));
		jobOperator.start(helloJob, date(13, "manual"));

		assertThat(jobInstances()).isEqualTo(2);
	}

	@Test
	void differentNonIdentifyingParameter_shouldStillBeTheSameJobInstance() throws Exception {
		jobOperator.start(helloJob, date(12, "manual"));

		// 只有 triggeredBy 不同：不參與識別，所以仍然是「10/12 已經做完」
		assertThatThrownBy(() -> jobOperator.start(helloJob, date(12, "scheduler")))
				.isInstanceOf(JobInstanceAlreadyCompleteException.class);
		assertThat(jobInstances()).isEqualTo(1);
	}

	@Test
	void runIdIncrementer_shouldCreateNewJobInstanceEveryTime() {
		jobOperator.startNextInstance(incrementedJob);
		jobOperator.startNextInstance(incrementedJob);

		assertThat(jobInstances()).isEqualTo(2);
		assertThat(jdbc.queryForList("""
				select parameter_value from batch_job_execution_params
				where parameter_name = 'run.id' order by job_execution_id
				""", String.class)).containsExactly("1", "2");
	}
}
