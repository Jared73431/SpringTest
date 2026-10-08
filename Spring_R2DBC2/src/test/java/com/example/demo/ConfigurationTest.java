package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;

@SpringBootTest
class ConfigurationTest extends PostgresContainerTestBase {

	@Autowired
	private FlywayProperties flywayProperties;

	@Autowired
	private DatabaseClient databaseClient;

	// 修正前 baseline-on-migrate=true + baseline-version=0：既有的 car 表會讓 V1 的 CREATE INDEX 失敗
	@Test
	void flyway_shouldNotBaselineOnMigrate() {
		assertThat(flywayProperties.isBaselineOnMigrate()).isFalse();
	}

	@Test
	void flyway_shouldCreateCarTable() {
		Long versions = databaseClient.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success")
				.map(row -> row.get(0, Long.class)).one().block();

		assertThat(versions).isEqualTo(1);
	}
}
