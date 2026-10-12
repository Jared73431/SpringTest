package com.example.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 所有整合測試共用一個 PostgreSQL container（只啟動一次），不會連到本機的資料庫。
 */
public abstract class PostgresContainerTestBase {

	@ServiceConnection
	protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	/** 書籍報表寫到暫存目錄，不會在專案目錄中產生檔案 */
	protected static final Path REPORT_DIRECTORY;

	static {
		POSTGRES.start();
		try {
			REPORT_DIRECTORY = Files.createTempDirectory("book-reports");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@DynamicPropertySource
	static void reportDirectory(DynamicPropertyRegistry registry) {
		registry.add("app.report.directory", REPORT_DIRECTORY::toString);
	}

	/** 清空執行紀錄，書籍與訂單恢復成 Flyway 範例資料的狀態 */
	protected static void resetData(DataSource dataSource) {
		new ResourceDatabasePopulator(new ClassPathResource("reset-data.sql")).execute(dataSource);
	}
}
