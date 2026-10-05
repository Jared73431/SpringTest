package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Baseline：用 OkHttp 的 MockWebServer 模擬 JSONPlaceholder，鎖定目前的行為，測試不再連外部網路。
 * 應用程式啟動時（CommandLineRunner）也會呼叫外部 API，因此用 Dispatcher 依路徑回應，而不是依序排隊的回應。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OKHttpControllerBaselineTest {

	static final MockWebServer SERVER = new MockWebServer();

	/** 外部 API 回應的狀態碼，測試中可以改變 */
	static volatile int upstreamStatus = 200;

	static {
		SERVER.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				if (upstreamStatus != 200) {
					return new MockResponse().setResponseCode(upstreamStatus);
				}
				if ("POST".equals(request.getMethod())) {
					return new MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json")
							.setBody("{\"id\": 101, \"userId\": 1, \"title\": \"new\", \"body\": \"b\"}");
				}
				return new MockResponse().setHeader("Content-Type", "application/json")
						.setBody("{\"id\": 1, \"userId\": 1, \"title\": \"t1\", \"body\": \"b1\"}");
			}
		});
		try {
			SERVER.start();
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@DynamicPropertySource
	static void baseUrl(DynamicPropertyRegistry registry) {
		registry.add("jsonplaceholder.base-url", () -> "http://localhost:" + SERVER.getPort());
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeEach
	void reset() throws InterruptedException {
		upstreamStatus = 200;
		// 清掉之前（包含啟動時）記錄的請求
		while (SERVER.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
		}
	}

	@Test
	void health_shouldReturnRunning() {
		assertThat(restTemplate.getForObject("/api/okhttp/health", String.class)).isEqualTo("OKHttp Service is running!");
	}

	@Test
	void testGet_shouldCallUpstreamAndReturnMessageOnly() throws InterruptedException {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/okhttp/test-get", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("GET 請求執行完成");
		assertThat(SERVER.takeRequest(1, TimeUnit.SECONDS).getPath()).isEqualTo("/posts/1");
	}

	@Test
	void testPost_shouldSendJsonBody() throws InterruptedException {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/okhttp/test-post", null, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		RecordedRequest request = SERVER.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getBody().readUtf8()).contains("\"userId\":1");
	}

	// [Potential Bug] 外部 API 回 500 時，Service 只印 log，API 仍回 200「執行完成」
	@Test
	void testGet_shouldStillReturnOk_whenUpstreamFails() {
		upstreamStatus = 500;

		ResponseEntity<String> response = restTemplate.getForEntity("/api/okhttp/test-get", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("執行完成");
	}

	@Test
	void testAsync_shouldReturnImmediately() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/okhttp/test-async", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("異步請求已啟動");
	}
}
