package com.example.demo;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 所有整合測試共用一個 PostgreSQL container（只啟動一次），不會連到本機的資料庫。
 */
public abstract class PostgresContainerTestBase {

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static {
		POSTGRES.start();
	}
}
