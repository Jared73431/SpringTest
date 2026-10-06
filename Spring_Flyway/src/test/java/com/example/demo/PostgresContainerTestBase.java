package com.example.demo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 整合測試共用的 PostgreSQL 容器：每次測試都從乾淨的空資料庫開始執行所有遷移，不會連到本機的資料庫。
 * Flyway 使用主要的 DataSource，因此只需要覆蓋 spring.datasource.*。
 */
public abstract class PostgresContainerTestBase {

	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}
}
