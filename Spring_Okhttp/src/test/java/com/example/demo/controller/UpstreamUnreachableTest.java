package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

/**
 * 外部 API 完全連不上（port 1 沒有服務在監聽）時：應用程式仍可啟動（DemoRunner 只記錄警告），
 * 同步與非同步 API 都回 503。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"jsonplaceholder.base-url=http://localhost:1",
		"demo.runner.enabled=true" })
@AutoConfigureTestRestTemplate
class UpstreamUnreachableTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void bothApis_shouldReturnServiceUnavailable_whenUpstreamUnreachable() {
		assertThat(restTemplate.getForEntity("/api/posts/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(restTemplate.getForEntity("/api/async/posts/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	// 舊的示範端點已移除
	@Test
	void legacyEndpoints_shouldReturnNotFound() {
		assertThat(restTemplate.getForEntity("/api/okhttp/test-get", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForEntity("/api/okhttp/health", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
