package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.SpringBatch4Application;
import com.example.demo.price.PriceUpdateJobConfig;

/**
 * 第 3 課：多個 Job 怎麼區分，以及一個 Job 裡的流程。
 *
 * <ul>
 * <li>每個 Job 有自己的名稱，JobRegistry 依名稱找到 Job；REST API 和命令列都是用名稱指定</li>
 * <li>啟動時自動執行（spring.batch.job.enabled=true）而且有多個 Job 時，必須用 spring.batch.job.name 指定</li>
 * <li>流程：Step 的 exit status 決定下一步（priceUpdateJob）</li>
 * </ul>
 *
 * 修正前用 @Qualifier("importUserJob4") 之類的 Bean 名稱注入每一個 Job 再分別啟動；Job 名稱與 Bean 名稱也不一定相同
 * （HelloJob8 的 Bean 叫 sampleJob）。
 */
@SpringBootTest
class L03_MultipleJobsTest extends PostgresContainerTestBase {

	@Autowired
	private JobRegistry jobRegistry;

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

	@Test
	void jobRegistry_shouldFindEveryJobByName() {
		assertThat(jobRegistry.getJobNames()).containsExactlyInAnyOrder("bookReportJob", "priceUpdateJob",
				"orderShippingJob");
		assertThat(jobRegistry.getJob("orderShippingJob").getName()).isEqualTo("orderShippingJob");
	}

	/** 等同 java -jar app.jar --spring.batch.job.enabled=true args...（不啟動 web） */
	private static void runFromCommandLine(String... args) {
		var arguments = new java.util.ArrayList<>(List.of("--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword(),
				"--app.report.directory=" + REPORT_DIRECTORY, "--spring.batch.job.enabled=true"));
		arguments.addAll(List.of(args));
		SpringApplication.exit(new SpringApplicationBuilder(SpringBatch4Application.class)
				.web(WebApplicationType.NONE).run(arguments.toArray(String[]::new)));
	}

	@Test
	void commandLine_shouldRequireAJobName_whenThereAreSeveralJobs() {
		assertThatThrownBy(L03_MultipleJobsTest::runFromCommandLine).rootCause()
				.hasMessageContaining("Job name must be specified in case of multiple jobs");
	}

	@Test
	void commandLine_shouldRunOnlyTheNamedJob() {
		runFromCommandLine("--spring.batch.job.name=orderShippingJob");

		assertThat(jdbc.queryForList("select job_name from batch_job_instance", String.class))
				.containsExactly("orderShippingJob");
	}

	private JobExecution runPriceUpdate(Path file) throws Exception {
		return jobOperator.start(priceUpdateJob, new JobParametersBuilder()
				.addString(PriceUpdateJobConfig.INPUT_FILE, file.toUri().toString())
				.toJobParameters());
	}

	private static List<String> stepNames(JobExecution execution) {
		return execution.getStepExecutions().stream()
				.sorted(java.util.Comparator.comparing(StepExecution::getId))
				.map(StepExecution::getStepName)
				.toList();
	}

	@Test
	void flow_shouldGoStraightToTheReport_whenNothingWasSkipped() throws Exception {
		var file = Files.writeString(tempDir.resolve("clean.csv"), "isbn,price\n9781617297571,49.00\n");

		var execution = runPriceUpdate(file);

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(stepNames(execution)).containsExactly("priceUpdateStep", "bookReportStep");
	}

	@Test
	void flow_shouldSendASkipNoticeFirst_whenLinesWereSkipped() throws Exception {
		var file = Files.writeString(tempDir.resolve("skips.csv"), "isbn,price\n9781617297571,abc\n");

		var execution = runPriceUpdate(file);

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(stepNames(execution)).containsExactly("priceUpdateStep", "skipNoticeStep", "bookReportStep");
	}

	// 跳過超過 skipLimit（10 行）：Step 失敗，Job 失敗，不產生報表
	@Test
	void flow_shouldFailTheJob_whenTheStepFails() throws Exception {
		var file = Files.writeString(tempDir.resolve("broken.csv"), "isbn,price\n" + "9781617297571,abc\n".repeat(11));

		var execution = runPriceUpdate(file);

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(stepNames(execution)).containsExactly("priceUpdateStep");
	}
}
