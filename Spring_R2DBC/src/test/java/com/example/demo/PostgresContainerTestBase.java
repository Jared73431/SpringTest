package com.example.demo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 所有整合測試共用一個 PostgreSQL container（static 初始化，整個測試執行期間只啟動一次），不會連到本機的資料庫。
 * R2DBC 與 Flyway（JDBC）分別指向同一個 container。
 */
public abstract class PostgresContainerTestBase {

	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
				+ POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
		registry.add("spring.r2dbc.username", POSTGRES::getUsername);
		registry.add("spring.r2dbc.password", POSTGRES::getPassword);
		registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
		registry.add("spring.flyway.user", POSTGRES::getUsername);
		registry.add("spring.flyway.password", POSTGRES::getPassword);
	}
}
