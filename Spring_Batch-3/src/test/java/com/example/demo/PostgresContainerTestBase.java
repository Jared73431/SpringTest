package com.example.demo;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 所有整合測試共用一個 PostgreSQL container（只啟動一次），不會連到本機的資料庫。
 */
public abstract class PostgresContainerTestBase {

	@ServiceConnection
	protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static {
		POSTGRES.start();
	}

	/**
	 * 回到「從來沒有匯入過」的狀態：person 表與執行紀錄要一起清空。
	 * 只清 person 時，執行紀錄仍然記得 importPersonJob 已經完成，再執行會被拒絕。
	 */
	protected static void deleteImportedDataAndHistory(JdbcTemplate jdbc) {
		jdbc.execute("""
				TRUNCATE person, batch_step_execution_context, batch_job_execution_context, batch_step_execution,
				         batch_job_execution_params, batch_job_execution, batch_job_instance
				""");
	}
}
