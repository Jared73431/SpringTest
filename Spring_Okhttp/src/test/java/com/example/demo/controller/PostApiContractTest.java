package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.demo.model.Post;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 用 OkHttp 官方的 MockWebServer（mockwebserver3）模擬 JSONPlaceholder，測試完全不連外部網路。
 * 「查詢一筆」的案例同步、非同步兩個版本都要通過，因此寫在這個抽象類別，由子類別提供路徑。
 *
 * MockWebServer 在 static 區塊啟動一次、所有測試類別共用（兩個子類別的設定相同，才能共用同一個 Spring context）。
 * 回應用自訂 Dispatcher 依「方法 + 路徑」決定，每個測試前清空。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
abstract class PostApiContractTest {

	static final MockWebServer SERVER = new MockWebServer();

	/** key："GET /posts/1" */
	static final Map<String, MockResponse> STUBS = new ConcurrentHashMap<>();

	static {
		SERVER.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return STUBS.getOrDefault(request.getMethod() + " " + request.getTarget(),
						new MockResponse.Builder().code(500).body("no stub").build());
			}
		});
		try {
			SERVER.start();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("jsonplaceholder.base-url", () -> "http://localhost:" + SERVER.getPort());
		// 讀取逾時縮短為 1 秒，逾時測試不必等太久
		registry.add("jsonplaceholder.read-timeout", () -> "1s");
		// 測試時不執行啟動示範
		registry.add("demo.runner.enabled", () -> "false");
	}

	static final String POST_1 = """
			{"id": 1, "userId": 1, "title": "t1", "body": "b1"}
			""";

	@Autowired
	TestRestTemplate restTemplate;

	/** 受測的「查詢一筆」路徑前綴 */
	abstract String basePath();

	static void stub(String methodAndPath, MockResponse response) {
		STUBS.put(methodAndPath, response);
	}

	static MockResponse json(int code, String body) {
		return new MockResponse.Builder().code(code).setHeader("Content-Type", "application/json").body(body).build();
	}

	@BeforeEach
	void resetStubs() throws InterruptedException {
		STUBS.clear();
		// 清掉之前測試記錄的請求，讓 takeRequest() 只拿到本測試的請求
		while (SERVER.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
		}
	}

	@Test
	void findById_shouldReturnPost_whenUpstreamFindsIt() {
		stub("GET /posts/1", json(200, POST_1));

		ResponseEntity<Post> response = restTemplate.getForEntity(basePath() + "/1", Post.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(new Post(1L, 1L, "t1", "b1"));
	}

	// 修正前：外部 API 失敗時，API 仍回 200「執行完成」
	@Test
	void findById_shouldReturnNotFoundProblemDetail_whenUpstreamReturnsNotFound() {
		stub("GET /posts/999", json(404, "{}"));

		ResponseEntity<String> response = restTemplate.getForEntity(basePath() + "/999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void findById_shouldReturnBadGateway_whenUpstreamReturnsServerError() {
		stub("GET /posts/1", json(500, "{}"));

		ResponseEntity<String> response = restTemplate.getForEntity(basePath() + "/1", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// 外部 API 超過讀取逾時（測試設定為 1 秒）仍未回應
	@Test
	void findById_shouldReturnServiceUnavailable_whenUpstreamTimesOut() {
		stub("GET /posts/1", new MockResponse.Builder().code(200).body(POST_1).headersDelay(2, TimeUnit.SECONDS).build());

		ResponseEntity<String> response = restTemplate.getForEntity(basePath() + "/1", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}
}
