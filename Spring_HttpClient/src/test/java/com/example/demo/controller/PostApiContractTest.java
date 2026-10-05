package com.example.demo.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.demo.model.Post;
import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * RestClient 與 WebClient 兩個版本的 API 必須有相同行為，因此測試案例只寫一次，
 * 由子類別提供路徑（/api/posts、/api/reactive/posts）。
 *
 * JSONPlaceholder 以 WireMock 模擬，測試完全不連外部網路。
 * WireMock 在 static 區塊啟動一次、所有測試類別共用：兩個子類別的設定相同，才能共用同一個 Spring context。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
abstract class PostApiContractTest {

	static final WireMockServer JSON_PLACEHOLDER = new WireMockServer(wireMockConfig().dynamicPort());

	static {
		JSON_PLACEHOLDER.start();
	}

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("jsonplaceholder.base-url", JSON_PLACEHOLDER::baseUrl);
		// 讀取逾時縮短為 1 秒，逾時測試不必等太久
		registry.add("jsonplaceholder.read-timeout", () -> "1s");
		// 測試時不執行啟動示範
		registry.add("demo.runner.enabled", () -> "false");
	}

	private static final String POST_1 = """
			{"id": 1, "userId": 1, "title": "t1", "body": "b1"}
			""";

	@Autowired
	TestRestTemplate restTemplate;

	/** 受測 API 的路徑 */
	abstract String basePath();

	@BeforeEach
	void resetStubs() {
		JSON_PLACEHOLDER.resetAll();
	}

	@Test
	void findById_shouldReturnPost_whenUpstreamFindsIt() {
		JSON_PLACEHOLDER.stubFor(get("/posts/1").willReturn(okJson(POST_1)));

		ResponseEntity<Post> response = restTemplate.getForEntity(basePath() + "/1", Post.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(new Post(1L, 1L, "t1", "b1"));
	}

	@Test
	void findAll_shouldReturnTypedPosts() {
		JSON_PLACEHOLDER.stubFor(get("/posts").willReturn(okJson("[" + POST_1 + "]")));

		ResponseEntity<Post[]> response = restTemplate.getForEntity(basePath(), Post[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsExactly(new Post(1L, 1L, "t1", "b1"));
	}

	@Test
	void create_shouldForwardJsonAndReturnCreatedWithLocation() {
		JSON_PLACEHOLDER.stubFor(post("/posts").willReturn(aResponse().withStatus(201)
				.withHeader("Content-Type", "application/json")
				.withBody("{\"id\": 101, \"userId\": 1, \"title\": \"new\", \"body\": \"b\"}")));

		ResponseEntity<Post> response = restTemplate.postForEntity(basePath(), Post.of(1L, "new", "b"), Post.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasPath(basePath() + "/101");
		assertThat(response.getBody().id()).isEqualTo(101L);
		JSON_PLACEHOLDER.verify(postRequestedFor(urlEqualTo("/posts"))
				.withRequestBody(equalToJson("{\"userId\": 1, \"title\": \"new\", \"body\": \"b\"}", true, true)));
	}

	@Test
	void update_shouldForwardPutAndReturnPost() {
		JSON_PLACEHOLDER.stubFor(put("/posts/1").willReturn(okJson(POST_1)));

		ResponseEntity<Post> response = restTemplate.exchange(basePath() + "/1", HttpMethod.PUT,
				new HttpEntity<>(new Post(1L, 1L, "t1", "b1")), Post.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JSON_PLACEHOLDER.verify(putRequestedFor(urlEqualTo("/posts/1")));
	}

	@Test
	void delete_shouldReturnNoContent() {
		JSON_PLACEHOLDER.stubFor(delete("/posts/1").willReturn(okJson("{}")));

		ResponseEntity<Void> response = restTemplate.exchange(basePath() + "/1", HttpMethod.DELETE, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		JSON_PLACEHOLDER.verify(deleteRequestedFor(urlEqualTo("/posts/1")));
	}

	// 修正前：外部 API 的 404 一律變成 500
	@Test
	void findById_shouldReturnNotFoundProblemDetail_whenUpstreamReturnsNotFound() {
		JSON_PLACEHOLDER.stubFor(get("/posts/999").willReturn(aResponse().withStatus(404).withBody("{}")));

		ResponseEntity<String> response = restTemplate.getForEntity(basePath() + "/999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void findById_shouldReturnBadGateway_whenUpstreamReturnsServerError() {
		JSON_PLACEHOLDER.stubFor(get("/posts/1").willReturn(aResponse().withStatus(500)));

		ResponseEntity<String> response = restTemplate.getForEntity(basePath() + "/1", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// 外部 API 超過讀取逾時（測試設定為 1 秒）仍未回應
	@Test
	void findById_shouldReturnServiceUnavailable_whenUpstreamTimesOut() {
		JSON_PLACEHOLDER.stubFor(get("/posts/1").willReturn(okJson(POST_1).withFixedDelay(2000)));

		ResponseEntity<String> response = restTemplate.getForEntity(basePath() + "/1", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}
}
