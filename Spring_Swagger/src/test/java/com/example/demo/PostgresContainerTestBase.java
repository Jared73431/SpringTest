package com.example.demo;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 所有整合測試共用一個 PostgreSQL container（只啟動一次），不會連到本機的資料庫。
 * \@ServiceConnection 讓 Spring Boot 直接使用 container 的連線資訊（datasource 與 Flyway）。
 */
public abstract class PostgresContainerTestBase {

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static {
		POSTGRES.start();
	}
}
