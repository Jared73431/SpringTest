package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 已經執行過的版本遷移檔<b>一個字都不能改</b>：Flyway 記錄了每個檔案的 checksum，
 * 啟動時（validate-on-migrate）發現不一致就拒絕執行。
 *
 * 做法：應用程式啟動時已對測試資料庫執行完所有遷移；把遷移檔複製到暫存資料夾，
 * 只在 V1.0.0 加上一行註解，再用同一個資料庫執行 validate。
 */
@SpringBootTest
class ChecksumValidationTest extends PostgresContainerTestBase {

	@Autowired
	private DataSource dataSource;

	@TempDir
	Path migrations;

	private Flyway flywayWith(String locations) {
		return Flyway.configure().dataSource(dataSource).locations(locations, "classpath:db/migration").load();
	}

	private void copyMigrations() throws IOException {
		for (Resource resource : new PathMatchingResourcePatternResolver()
				.getResources("classpath:doc/migration/common/*.sql")) {
			Files.write(migrations.resolve(resource.getFilename()), resource.getContentAsByteArray());
		}
	}

	@Test
	void validate_shouldPass_whenMigrationFilesUnchanged() throws IOException {
		copyMigrations();

		flywayWith("filesystem:" + migrations).validate();
	}

	@Test
	void validate_shouldFail_whenAppliedMigrationIsModified() throws IOException {
		copyMigrations();
		Path v100 = migrations.resolve("V1.0.0__create_javastack.sql");
		Files.writeString(v100, "-- 只加了一行註解\n" + Files.readString(v100, StandardCharsets.UTF_8),
				StandardCharsets.UTF_8);

		assertThatThrownBy(() -> flywayWith("filesystem:" + migrations).validate())
				.isInstanceOf(FlywayValidateException.class)
				.hasMessageContaining("checksum mismatch")
				.hasMessageContaining("1.0.0");
	}

	@Test
	void migrate_shouldBeNoOp_whenRunAgain() throws IOException {
		copyMigrations();

		assertThat(flywayWith("filesystem:" + migrations).migrate().migrationsExecuted).isZero();
	}
}
