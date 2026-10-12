package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Baseline：鎖定重構前的行為。
 *
 * <p>
 * 用同一個 PostgreSQL（Testcontainers）把應用程式啟動兩次，觀察 JobRepository 的 BATCH_* 表。
 * 原本的 contextLoads 會連到本機的 localhost:5432/test，已移除。
 */
@ExtendWith(OutputCaptureExtension.class)
class BatchRepositoryBaselineTest {

	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static JdbcTemplate jdbc;

	@BeforeAll
	static void startDatabase() {
		POSTGRES.start();
		jdbc = new JdbcTemplate(
				new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
	}

	@AfterAll
	static void stopDatabase() {
		POSTGRES.stop();
	}

	private static void startApplication() {
		new SpringApplicationBuilder(SpringBatch2Application.class)
				.run("--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
						"--spring.datasource.username=" + POSTGRES.getUsername(),
						"--spring.datasource.password=" + POSTGRES.getPassword(), "--server.port=0")
				.close();
	}

	private static int count(String text, String word) {
		return (int) Pattern.compile(Pattern.quote(word)).matcher(text).results().count();
	}

	// Boot 3.5（Spring Batch 5）時：第 2 次啟動新增一筆 exit code 為 NOOP 的 JobExecution，Step 不執行，程式正常啟動。
	// Boot 4（Spring Batch 6）改成拋出 JobInstanceAlreadyCompleteException，程式啟動失敗。
	@Test
	void secondStart_shouldFail_whenJobInstanceAlreadyCompleted(CapturedOutput output) {
		// 第 1 次啟動
		startApplication();

		// spring.batch.job.enable=false 拼錯（正確是 enabled），沒有作用：Job 仍然執行
		assertThat(count(output.getOut(), "Hello world2!")).isEqualTo(1);
		assertThat(jdbc.queryForList("select job_name from batch_job_instance", String.class))
				.containsExactly("HelloJob8");
		assertThat(jdbc.queryForList("select status from batch_job_execution", String.class))
				.containsExactly("COMPLETED");
		assertThat(jdbc.queryForList("select step_name from batch_step_execution", String.class))
				.containsExactly("helloStep8");

		// 第 2 次啟動，同一個資料庫。initialize-schema=always：BATCH_* 表已經存在，建表不會讓啟動失敗；
		// 失敗的是執行 Job：沒有 JobParameters → 同一個 JobInstance，而它已經 COMPLETED
		assertThatThrownBy(BatchRepositoryBaselineTest::startApplication)
				.hasRootCauseInstanceOf(JobInstanceAlreadyCompleteException.class);

		assertThat(count(output.getOut(), "Hello world2!")).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from batch_job_instance", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from batch_job_execution", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from batch_step_execution", Integer.class)).isEqualTo(1);
	}
}
