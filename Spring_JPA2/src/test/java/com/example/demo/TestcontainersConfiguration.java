package com.example.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 測試用的 PostgreSQL 容器。
 * 測試時由 Testcontainers 啟動臨時資料庫，@ServiceConnection 會自動把連線資訊
 * 設定給 Spring Boot，不會連到本機開發用的資料庫。
 * 使用方式：在測試類別加上 @Import(TestcontainersConfiguration.class)
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer("postgres:15-alpine");
	}
}
