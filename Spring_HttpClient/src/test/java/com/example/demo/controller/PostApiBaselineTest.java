package com.example.demo.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Baseline：用 WireMock 模擬 JSONPlaceholder，鎖定目前的行為，測試不再連外部網路。
 * DemoRunner 會在應用程式啟動時呼叫外部 API，因此必須在 Spring 啟動前（@BeforeAll）先準備好回應，否則啟動失敗。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class PostApiBaselineTest {

	@RegisterExtension
	static WireMockExtension jsonPlaceholder = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort()).build();

	@DynamicPropertySource
	static void baseUrl(DynamicPropertyRegistry registry) {
		registry.add("jsonplaceholder.base-url", jsonPlaceholder::baseUrl);
	}

	private static final String POST_1 = """
			{"id": 1, "userId": 1, "title": "t1", "body": "b1"}
			""";

	// WireMockExtension 預設在每個測試前清空 stub，所以 @BeforeEach 也要重新設定
	@BeforeAll
	static void stubForDemoRunner() {
		stubDefaults();
	}

	@BeforeEach
	void stubForEachTest() {
		stubDefaults();
	}

	private static void stubDefaults() {
		jsonPlaceholder.stubFor(get("/posts/1").willReturn(okJson(POST_1)));
		jsonPlaceholder.stubFor(get("/posts").willReturn(okJson("[" + POST_1 + "]")));
		jsonPlaceholder.stubFor(post("/posts").willReturn(aResponse().withStatus(201)
				.withHeader("Content-Type", "application/json")
				.withBody("{\"id\": 101, \"userId\": 1, \"title\": \"new\", \"body\": \"b\"}")));
		jsonPlaceholder.stubFor(put("/posts/1").willReturn(okJson(POST_1)));
		jsonPlaceholder.stubFor(delete("/posts/1").willReturn(okJson("{}")));
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void getPost_shouldReturnPost() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/posts/1", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"title\":\"t1\"");
	}

	@Test
	void getAllPosts_shouldReturnPosts() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/posts", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"id\":1");
	}

	@Test
	void createPost_shouldForwardBodyAndReturnOk() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/posts",
				java.util.Map.of("userId", 1, "title", "new", "body", "b"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"id\":101");
		jsonPlaceholder.verify(postRequestedFor(urlEqualTo("/posts"))
				.withRequestBody(equalToJson("{\"userId\": 1, \"title\": \"new\", \"body\": \"b\"}", true, true)));
	}

	@Test
	void updatePost_shouldReturnOk() {
		ResponseEntity<String> response = restTemplate.exchange("/api/posts/1", HttpMethod.PUT,
				new HttpEntity<>(java.util.Map.of("userId", 1, "title", "t1", "body", "b1")), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void deletePost_shouldReturnOk() {
		ResponseEntity<String> response = restTemplate.exchange("/api/posts/1", HttpMethod.DELETE, null,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	// [Potential Bug] 外部 API 回 404 時，WebClientResponseException 沒有處理，一律變成 500
	@Test
	void getPost_shouldReturnInternalServerError_whenUpstreamReturnsNotFound() {
		jsonPlaceholder.stubFor(get("/posts/999").willReturn(aResponse().withStatus(404).withBody("{}")));

		ResponseEntity<String> response = restTemplate.getForEntity("/api/posts/999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void testGet_shouldReturnPost() {
		ResponseEntity<String> response = restTemplate.getForEntity("/test/get/1", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"title\":\"t1\"");
	}

	// GET 卻會在外部 API 新增資料（有副作用）
	@Test
	void createSample_shouldCreatePostViaGet() {
		ResponseEntity<String> response = restTemplate.getForEntity("/test/create-sample", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		jsonPlaceholder.verify(postRequestedFor(urlEqualTo("/posts")));
	}
}
