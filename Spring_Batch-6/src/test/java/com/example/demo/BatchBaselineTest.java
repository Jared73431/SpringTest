package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Baseline：鎖定合併前 Spring_Batch-6（包含 Batch-4、5 的全部內容）的行為。
 *
 * <p>
 * 原本的 contextLoads 會連到本機的 localhost:5432/test，已移除。BATCH_* 表用 Spring Batch 內附的建表 SQL 建立
 * （專案設定 initialize-schema=never）；book 表沒有任何建立資料的機制，用 baseline-books.sql 建立 6 本書。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BatchBaselineTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
			.withInitScripts("org/springframework/batch/core/schema-postgresql.sql", "baseline-books.sql");

	static {
		POSTGRES.start();
	}

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private TestRestTemplate rest;

	// BookItemWriter 的 CSV 寫到相對路徑 output/（執行目錄下）
	@AfterAll
	static void deleteCsvOutput() {
		FileSystemUtils.deleteRecursively(new File("output"));
	}

	private Map<String, Long> executionsPerJob() {
		return jdbc.queryForList("""
				select i.job_name from batch_job_instance i join batch_job_execution e using (job_instance_id)
				where e.end_time is not null
				""", String.class).stream().collect(Collectors.groupingBy(name -> name, Collectors.counting()));
	}

	private void awaitStartupJobs() {
		// 所有 @Scheduled(fixedRate) 都沒有 initialDelay：啟動後立刻各執行一次（排程只有 1 條執行緒，依序執行）
		await().atMost(Duration.ofSeconds(60)).until(() -> executionsPerJob().size() == 8);
	}

	private long stepReadCount(String jobName, String stepName) {
		return jdbc.queryForObject("""
				select s.read_count from batch_step_execution s
				join batch_job_execution e using (job_execution_id) join batch_job_instance i using (job_instance_id)
				where i.job_name = ? and s.step_name = ? order by s.step_execution_id limit 1
				""", Long.class, jobName, stepName);
	}

	@Test
	void startup_shouldRunEveryFixedRateJobOnce() {
		awaitStartupJobs();

		assertThat(executionsPerJob()).containsOnlyKeys("importUserJob4", "healthInsuranceJob", "HelloJob8",
				"bookRepositoryJob", "bookJpaJob", "bookJdbcJob", "bookComprehensiveJob", "bookConditionalJob");
		assertThat(jdbc.queryForList("select distinct status from batch_job_execution", String.class))
				.containsExactly("COMPLETED");
		assertThat(jdbc.queryForObject("select count(*) from person", Integer.class)).isEqualTo(4);
		assertThat(jdbc.queryForObject("select count(*) from health_insurance", Integer.class)).isEqualTo(23);
	}

	// 每種 Reader 依照自己的查詢條件讀取 baseline-books.sql 的 6 本書
	@Test
	void readers_shouldSelectBooksByTheirQueries() {
		awaitStartupJobs();

		assertThat(stepReadCount("bookRepositoryJob", "bookRepositoryStep")).isEqualTo(6); // findAll
		assertThat(stepReadCount("bookJpaJob", "bookJpaStep")).isEqualTo(3); // cost > 50
		assertThat(stepReadCount("bookJdbcJob", "bookJdbcStep")).isEqualTo(5); // cost > 30
		// cost 20~100 且 year >= 2010；條件流程：完成後接著執行 bookRepositoryStep
		assertThat(stepReadCount("bookConditionalJob", "bookComplexStep")).isEqualTo(3);
		assertThat(stepReadCount("bookConditionalJob", "bookRepositoryStep")).isEqualTo(6);
	}

	// GET 會觸發 Job，並且等 Job 執行完才回應（同步）
	@Test
	void getEndpoint_shouldRunTheJobSynchronously() {
		awaitStartupJobs();
		long before = executionsPerJob().get("bookJdbcJob");

		var response = rest.getForEntity("/api/batch/books/jdbc", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("status", "COMPLETED").containsEntry("jobName", "bookJdbcJob");
		assertThat(executionsPerJob().get("bookJdbcJob")).isEqualTo(before + 1);
	}

	// CSV writer 是 singleton，檔名的時間戳在啟動時就決定：JPA Step 不論執行幾次，都覆蓋同一個檔案
	@Test
	void csvWriter_shouldOverwriteTheSameFile_whenJpaStepRunsAgain() throws Exception {
		awaitStartupJobs();
		rest.getForEntity("/api/batch/books/jpa", Map.class);

		var files = new File("output").listFiles();
		assertThat(files).hasSize(1);
		assertThat(Files.readAllLines(files[0].toPath())).containsExactly(
				"ID,ISBN,Title,Author,Year,Publisher,Cost,Category,IsProcessed",
				"6,1006,MODERN JAVA,Raoul Urma,2021,MANNING,120.0,NEW_RELEASE,true",
				"1,1001,SPRING IN ACTION,Craig Walls,2022,MANNING,59.0,NEW_RELEASE,true",
				"3,1003,EFFECTIVE JAVA,Joshua Bloch,2018,ADDISON,55.0,RECENT,true");
	}
}
