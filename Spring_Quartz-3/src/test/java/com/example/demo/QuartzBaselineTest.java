package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Baseline：鎖定合併前 Spring_Quartz-3 的行為。
 *
 * <p>
 * 資料庫網址寫死在 application.properties（localhost:5432/quartz）。為了不連到本機資料庫，
 * 測試覆寫 quartzProperties 這個 Bean，改指向 Testcontainers；qrtz_* 資料表用 Quartz jar 內附的建表 SQL 建立
 * （專案本身沒有建表機制）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.main.allow-bean-definition-overriding=true")
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QuartzBaselineTest {

	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
			.withInitScript("org/quartz/impl/jdbcjobstore/tables_postgres.sql");

	static {
		POSTGRES.start();
	}

	@TestConfiguration
	static class ContainerQuartzProperties {
		@Bean
		@Primary
		Properties quartzProperties() throws Exception {
			Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
			properties.setProperty("org.quartz.dataSource.myDS.URL", POSTGRES.getJdbcUrl());
			properties.setProperty("org.quartz.dataSource.myDS.user", POSTGRES.getUsername());
			properties.setProperty("org.quartz.dataSource.myDS.password", POSTGRES.getPassword());
			return properties;
		}
	}

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private Scheduler scheduler;

	@Test
	void add_shouldStoreJobInDatabaseAndRunIt(CapturedOutput output) throws Exception {
		assertThat(rest.getForObject("/quartz/add", String.class)).isEqualTo("ok");

		assertThat(scheduler.checkExists(JobKey.jobKey("myJob", "myGroup"))).isTrue();
		await().atMost(Duration.ofSeconds(10)).until(() -> output.getOut().contains("HelloJob----"));
		rest.getForObject("/quartz/remove", String.class);
	}

	// [Potential Bug] 名稱寫死成 myJob：第二次新增失敗（ObjectAlreadyExistsException），例外被 printStackTrace 吞掉，仍然回傳 ok
	@Test
	void add_shouldStillReturnOk_whenJobAlreadyExists(CapturedOutput output) {
		rest.getForObject("/quartz/add", String.class);

		assertThat(rest.getForObject("/quartz/add", String.class)).isEqualTo("ok");
		assertThat(output.getAll()).contains("ObjectAlreadyExistsException");
		rest.getForObject("/quartz/remove", String.class);
	}

	@Test
	void remove_shouldDeleteJob() throws Exception {
		rest.getForObject("/quartz/add", String.class);

		assertThat(rest.getForObject("/quartz/remove", String.class)).isEqualTo("ok");
		assertThat(scheduler.checkExists(JobKey.jobKey("myJob", "myGroup"))).isFalse();
	}

	// [Potential Bug] shutdown() 是永久關閉：之後 start() 會失敗 → 500
	@Test
	void start_shouldFail_afterShutdown() {
		assertThat(rest.getForObject("/quartz/shutdown", String.class)).isEqualTo("ok");

		assertThat(rest.getForEntity("/quartz/start", String.class).getStatusCode())
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
