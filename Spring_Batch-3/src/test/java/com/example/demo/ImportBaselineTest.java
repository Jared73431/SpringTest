package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import org.testcontainers.containers.PostgreSQLContainer;

import com.example.demo.batch.Scheduled.BatchScheduler;

/**
 * Baseline：鎖定重構前的行為。
 *
 * <p>
 * 原本的 contextLoads 會連到本機的 localhost:5432/test，已移除。專案設定 spring.batch.jdbc.initialize-schema=never，
 * 依賴資料庫中已經存在的 BATCH_* 表，所以 container 用 Spring Batch 內附的建表 SQL 初始化。
 */
@SpringBootTest
class ImportBaselineTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
			.withInitScript("org/springframework/batch/core/schema-postgresql.sql");

	static {
		POSTGRES.start();
	}

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private BatchScheduler scheduler;

	@Autowired
	private JobExplorer jobExplorer;

	private int persons() {
		return jdbc.queryForObject("select count(*) from person", Integer.class);
	}

	@Test
	void everyScheduledRun_shouldImportTheSameCsvAgain() throws Exception {
		// @Scheduled(fixedRate) 沒有 initialDelay：應用程式啟動後立刻執行第一次
		await().atMost(Duration.ofSeconds(30)).until(() -> persons() == 4);
		assertThat(jdbc.queryForList("select first_name || ' ' || last_name from person order by id", String.class))
				.containsExactly("JILL DOE", "JOE DONE", "JUSTIN DOE", "JANE DOE");

		// 5 分鐘後的下一次（直接呼叫，不必等）：參數 time 每次不同 → 新的 JobInstance → 同一份 CSV 再匯入一次
		scheduler.perform();

		assertThat(persons()).isEqualTo(8);
		var instances = jobExplorer.getJobInstances("importUserJob4", 0, 10);
		assertThat(instances).hasSize(2);
		assertThat(instances).allSatisfy(instance -> assertThat(jobExplorer.getJobExecutions(instance))
				.singleElement().satisfies(execution -> {
					assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
					assertThat(execution.getStepExecutions()).singleElement().satisfies(step -> {
						assertThat(step.getStepName()).isEqualTo("step14");
						assertThat(step.getReadCount()).isEqualTo(4);
						assertThat(step.getWriteCount()).isEqualTo(4);
						// chunk(3)：4 筆分成 3 + 1，commit 2 次
						assertThat(step.getCommitCount()).isEqualTo(2);
					});
				}));
	}
}
