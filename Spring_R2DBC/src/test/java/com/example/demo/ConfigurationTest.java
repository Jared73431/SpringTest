package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.r2dbc.core.DatabaseClient;

@SpringBootTest
class ConfigurationTest extends PostgresContainerTestBase {

	@Autowired
	private R2dbcProperties r2dbcProperties;

	@Autowired
	private DatabaseClient databaseClient;

	@Autowired
	private ApplicationContext context;

	// 修正前以 Spring MVC 執行，Service 內用 block() 等待 R2DBC 的結果；現在只有 WebFlux
	@Test
	void context_shouldBeReactive() {
		assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
	}

	// 修正前寫成 spring.pool.*，沒有綁定到任何設定；改成 spring.r2dbc.pool.* 後才生效
	@Test
	void poolProperties_shouldBeBound() {
		R2dbcProperties.Pool pool = r2dbcProperties.getPool();

		assertThat(pool.getInitialSize()).isEqualTo(5);
		assertThat(pool.getMaxSize()).isEqualTo(10);
		assertThat(pool.getMaxCreateConnectionTime()).isEqualTo(Duration.ofSeconds(2));
	}

	// 資料表由 Flyway 建立（修正前沒有 schema 檔，要在本機手動建表）
	@Test
	void flyway_shouldCreateProductTable() {
		Long versions = databaseClient.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success")
				.map(row -> row.get(0, Long.class)).one().block();
		Long products = databaseClient.sql("SELECT COUNT(*) FROM product").map(row -> row.get(0, Long.class)).one()
				.block();

		assertThat(versions).isEqualTo(2);
		assertThat(products).isZero();
	}
}
