package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.time.Duration;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * REST API：用名稱啟動 Job（背景執行），再查詢執行狀態。
 */
@SpringBootTest
@AutoConfigureMockMvc
class JobApiTest extends PostgresContainerTestBase {

	private static final String PRICE_FILE = """
			{"input.file": "classpath:price-updates/price-update-2026-10-12.csv"}
			""";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanUp() {
		resetData(dataSource);
	}

	/** 啟動 Job，回傳 executionId */
	private long launch(String jobName, String body) throws Exception {
		var request = post("/api/jobs/{jobName}/executions", jobName);
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		var response = mvc.perform(request)
				.andExpect(status().isAccepted())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/jobs/executions/")))
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private String awaitFinished(long executionId) {
		return await().atMost(Duration.ofSeconds(30)).until(() -> {
			var json = mvc.perform(get("/api/jobs/executions/{id}", executionId)).andReturn().getResponse()
					.getContentAsString();
			String status = JsonPath.read(json, "$.status");
			return status.equals("COMPLETED") || status.equals("FAILED") ? json : null;
		}, json -> json != null);
	}

	@Test
	void jobs_shouldListEveryJobByName() throws Exception {
		mvc.perform(get("/api/jobs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").value(org.hamcrest.Matchers.contains("bookReportJob", "orderShippingJob",
						"priceUpdateJob")));
	}

	@Test
	void bookReportJob_shouldWriteOneReportFilePerExecution() throws Exception {
		long first = launch("bookReportJob", null);
		awaitFinished(first);
		// 有 timestamp incrementer：沒有參數也能再執行一次
		long second = launch("bookReportJob", null);
		String json = awaitFinished(second);

		assertThat((String) JsonPath.read(json, "$.status")).isEqualTo("COMPLETED");
		for (long id : new long[] { first, second }) {
			var report = REPORT_DIRECTORY.resolve("book-report-" + id + ".csv");
			assertThat(Files.readAllLines(report)).hasSize(9) // 標題 + 8 本書
					.first().isEqualTo("isbn,title,author,publishedYear,publisher,price,category");
		}
	}

	@Test
	void priceUpdateJob_shouldSkipBadLinesAndThenWriteTheNewReport() throws Exception {
		String json = awaitFinished(launch("priceUpdateJob", PRICE_FILE));

		assertThat((String) JsonPath.read(json, "$.status")).isEqualTo("COMPLETED");
		assertThat((java.util.List<String>) JsonPath.read(json, "$.steps[*].name"))
				.containsExactly("priceUpdateStep", "skipNoticeStep", "bookReportStep");
		assertThat((String) JsonPath.read(json, "$.steps[0].exitCode")).isEqualTo("COMPLETED WITH SKIPS");
		assertThat((Integer) JsonPath.read(json, "$.steps[0].writeCount")).isEqualTo(4);
		assertThat((Integer) JsonPath.read(json, "$.steps[0].skipCount")).isEqualTo(2);
		assertThat(jdbc.queryForObject("select price from book where isbn = '9781617297571'", String.class))
				.isEqualTo("49.00");
	}

	@Test
	void priceUpdateJob_shouldBeRejected_whenTheSameFileWasAlreadyApplied() throws Exception {
		awaitFinished(launch("priceUpdateJob", PRICE_FILE));

		mvc.perform(post("/api/jobs/priceUpdateJob/executions").contentType(MediaType.APPLICATION_JSON)
				.content(PRICE_FILE))
				.andExpect(status().isConflict());
	}

	@Test
	void priceUpdateJob_shouldReturn400_whenInputFileIsMissing() throws Exception {
		mvc.perform(post("/api/jobs/priceUpdateJob/executions"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("input.file")));
	}

	@Test
	void unknownJob_shouldReturn404() throws Exception {
		mvc.perform(post("/api/jobs/noSuchJob/executions")).andExpect(status().isNotFound());
		mvc.perform(get("/api/jobs/executions/999999")).andExpect(status().isNotFound());
	}
}
