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
 * 用和正式執行相同的方式啟動應用程式（等同 java -jar app.jar），做完就結束。
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringBatch3ApplicationTests extends PostgresContainerTestBase {

	private final JdbcTemplate jdbc = new JdbcTemplate(
			new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));

	@BeforeEach
	void cleanUp() {
		// 第一次啟動前表還不存在（由 Flyway 建立），所以只在表已經存在時清空
		if (jdbc.queryForObject("select to_regclass('person') is not null", Boolean.class)) {
			deleteImportedDataAndHistory(jdbc);
		}
	}

	private static int run() {
		return SpringApplication.exit(new SpringApplicationBuilder(SpringBatch3Application.class).run(
				"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword()));
	}

	@Test
	void run_shouldImportCsvInUpperCase(CapturedOutput output) {
		assertThat(run()).isZero();

		assertThat(jdbc.queryForList("select first_name || ' ' || last_name from person order by id", String.class))
				.containsExactly("JILL DOE", "JOE DONE", "JUSTIN DOE", "JANE DOE");
		assertThat(output.getOut())
				.contains("Step importPersonStep: read=4, written=4, commits=2")
				.contains("Total records in person table: 4");
	}

	// 沒有 JobParameters → 永遠是同一個 JobInstance。做完之後再執行會被拒絕，所以不會重複匯入；
	// 想要每次都能執行，可以加上時間戳參數（之後的 Batch 模組）
	@Test
	void secondRun_shouldBeRejected_andNotImportAgain() {
		run();

		assertThatThrownBy(SpringBatch3ApplicationTests::run)
				.hasRootCauseInstanceOf(JobInstanceAlreadyCompleteException.class);
		assertThat(jdbc.queryForObject("select count(*) from person", Integer.class)).isEqualTo(4);
	}
}
