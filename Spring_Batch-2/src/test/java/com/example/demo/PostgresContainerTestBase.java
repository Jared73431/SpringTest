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

	/** 清空執行紀錄，讓每個測試從沒有任何 JobInstance 開始（BATCH_* 表本身由 Flyway 建立，不刪除） */
	protected static void deleteBatchHistory(JdbcTemplate jdbc) {
		jdbc.execute("""
				TRUNCATE batch_step_execution_context, batch_job_execution_context, batch_step_execution,
				         batch_job_execution_params, batch_job_execution, batch_job_instance
				""");
	}
}
