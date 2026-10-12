package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 用和正式執行相同的方式啟動應用程式：命令列參數 → JobParameters，做完就結束。
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringBatch2ApplicationTests extends PostgresContainerTestBase {

	private final JdbcTemplate jdbc = new JdbcTemplate(
			new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));

	@BeforeEach
	void cleanUp() {
		// 第一次啟動前 BATCH_* 表還不存在（由 Flyway 建立），所以只在表已經存在時清空
		if (jdbc.queryForObject("select to_regclass('batch_job_instance') is not null", Boolean.class)) {
			deleteBatchHistory(jdbc);
		}
	}

	/** 等同 java -jar app.jar args...，回傳 exit code */
	private static int run(String... args) {
		var arguments = new String[args.length + 3];
		arguments[0] = "--spring.datasource.url=" + POSTGRES.getJdbcUrl();
		arguments[1] = "--spring.datasource.username=" + POSTGRES.getUsername();
		arguments[2] = "--spring.datasource.password=" + POSTGRES.getPassword();
		System.arraycopy(args, 0, arguments, 3, args.length);
		return SpringApplication.exit(new SpringApplicationBuilder(SpringBatch2Application.class).run(arguments));
	}

	@Test
	void run_shouldExecuteHelloJobAndRecordItInDatabase(CapturedOutput output) {
		assertThat(run("date=2026-10-12")).isZero();

		assertThat(output.getOut()).contains("Hello world2! jobParameters={date=2026-10-12}");
		assertThat(jdbc.queryForList("select job_name from batch_job_instance", String.class))
				.containsExactly("helloJob");
		assertThat(jdbc.queryForList("select step_name from batch_step_execution", String.class))
				.containsExactly("helloStep");
		// BATCH_* 表是 Flyway 建立的
		assertThat(jdbc.queryForList("select version from flyway_schema_history", String.class)).containsExactly("1");
	}

	// 同一天執行第二次會被拒絕：防止同一天的資料被處理兩次
	@Test
	void run_shouldFail_whenSameParametersAlreadyCompleted() {
		run("date=2026-10-12");

		assertThatThrownBy(() -> run("date=2026-10-12"))
				.hasRootCauseInstanceOf(JobInstanceAlreadyCompleteException.class);
		assertThat(jdbc.queryForObject("select count(*) from batch_job_execution", Integer.class)).isEqualTo(1);
	}

	@Test
	void run_shouldCreateNewJobInstance_whenParametersDiffer() {
		run("date=2026-10-12");
		run("date=2026-10-13");

		assertThat(jdbc.queryForObject("select count(*) from batch_job_instance", Integer.class)).isEqualTo(2);
	}
}
