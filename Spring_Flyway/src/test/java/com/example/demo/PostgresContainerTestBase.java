package com.example.demo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 整合測試共用的 PostgreSQL 容器：每次測試都從乾淨的空資料庫開始執行所有遷移，不會連到本機的資料庫。
 *
 * 同時覆蓋 spring.datasource.* 與 spring.flyway.*：設定檔另外指定了 spring.flyway.url，
 * 只覆蓋 datasource 的話，Flyway 仍會依 spring.flyway.url 連到本機資料庫執行遷移。
 */
public abstract class PostgresContainerTestBase {

	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
		registry.add("spring.flyway.user", POSTGRES::getUsername);
		registry.add("spring.flyway.password", POSTGRES::getPassword);
	}
}
