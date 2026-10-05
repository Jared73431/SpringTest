package com.example.demo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 整合測試共用的 PostgreSQL 容器。
 * 測試時由 Testcontainers 啟動一個臨時資料庫，不會連到本機開發用的資料庫。
 */
public abstract class PostgresContainerTestBase {

	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

	static {
		// 所有測試類別共用同一個容器，JVM 結束時由 Testcontainers 自動清除
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		// 測試不連 Eureka 註冊中心
		registry.add("eureka.client.enabled", () -> "false");
	}
}
