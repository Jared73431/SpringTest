package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 1 課：執行一次 Job，6 張 BATCH_* 表各記錄了什麼？
 *
 * <pre>
 * BATCH_JOB_INSTANCE            「哪一個 Job + 哪一組識別參數」：同一組參數只會有一列
 *  └─ BATCH_JOB_EXECUTION       每「執行一次」一列：狀態、開始與結束時間、exit code
 *      ├─ BATCH_JOB_EXECUTION_PARAMS     這次執行的參數（含型別、是否用來識別 JobInstance）
 *      ├─ BATCH_JOB_EXECUTION_CONTEXT    Job 層級的 ExecutionContext（重新執行時用來接續）
 *      └─ BATCH_STEP_EXECUTION           每個 Step 每執行一次一列：讀、寫、commit 次數
 *          └─ BATCH_STEP_EXECUTION_CONTEXT   Step 層級的 ExecutionContext（例如讀到第幾筆）
 * </pre>
 *
 * 用 psql 或 pgAdmin 查詢這些表，就能知道每一次批次的結果，不需要另外寫 log。
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class L01_MetadataTablesTest extends PostgresContainerTestBase {

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job helloJob;

	@Autowired
	private JdbcTemplate jdbc;

	private JobExecution execution;

	@BeforeEach
	void runHelloJobOnce() throws Exception {
		deleteBatchHistory(jdbc);
		var parameters = new JobParametersBuilder()
				.addLocalDate("date", LocalDate.of(2026, 10, 12))
				// 第三個參數 false：不用來識別 JobInstance（第 2 課）
				.addString("triggeredBy", "manual", false)
				.toJobParameters();
		execution = jobOperator.start(helloJob, parameters);
	}

	@Test
	void jobInstance_shouldStoreJobNameAndHashOfIdentifyingParameters() {
		var instance = jdbc.queryForMap("select job_instance_id, job_name, job_key from batch_job_instance");

		assertThat(instance.get("job_instance_id")).isEqualTo(execution.getJobInstance().getId());
		assertThat(instance.get("job_name")).isEqualTo("helloJob");
		// job_key 是識別參數的 MD5：用來判斷「這組參數是不是已經執行過」
		assertThat((String) instance.get("job_key")).hasSize(32);
	}

	@Test
	void jobExecution_shouldStoreStatusAndTimes() {
		var row = jdbc.queryForMap("select * from batch_job_execution");

		assertThat(row.get("job_execution_id")).isEqualTo(execution.getId());
		assertThat(row.get("job_instance_id")).isEqualTo(execution.getJobInstance().getId());
		assertThat(row.get("status")).isEqualTo("COMPLETED");
		assertThat(row.get("exit_code")).isEqualTo("COMPLETED");
		assertThat(row.get("start_time")).isNotNull();
		assertThat(row.get("end_time")).isNotNull();
	}

	@Test
	void jobExecutionParams_shouldStoreTypeAndWhetherIdentifying() {
		List<Map<String, Object>> params = jdbc.queryForList("""
				select parameter_name, parameter_type, parameter_value, identifying
				from batch_job_execution_params order by parameter_name
				""");

		assertThat(params).containsExactly(
				Map.of("parameter_name", "date", "parameter_type", "java.time.LocalDate",
						"parameter_value", "2026-10-12", "identifying", "Y"),
				Map.of("parameter_name", "triggeredBy", "parameter_type", "java.lang.String",
						"parameter_value", "manual", "identifying", "N"));
	}

	@Test
	void stepExecution_shouldStoreCountsForEachStep() {
		var step = jdbc.queryForMap("select * from batch_step_execution");

		assertThat(step.get("job_execution_id")).isEqualTo(execution.getId());
		assertThat(step.get("step_name")).isEqualTo("helloStep");
		assertThat(step.get("status")).isEqualTo("COMPLETED");
		// Tasklet 沒有讀寫資料；整個 Tasklet 在一個交易中執行，commit 一次
		assertThat(step.get("read_count")).isEqualTo(0L);
		assertThat(step.get("write_count")).isEqualTo(0L);
		assertThat(step.get("commit_count")).isEqualTo(1L);
	}

	@Test
	void executionContexts_shouldHaveOneRowForJobAndOneForStep() {
		assertThat(jdbc.queryForObject("select count(*) from batch_job_execution_context", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from batch_step_execution_context", Integer.class)).isEqualTo(1);
	}
}
