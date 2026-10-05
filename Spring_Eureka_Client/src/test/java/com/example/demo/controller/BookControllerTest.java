package com.example.demo.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Client API 的整合測試：啟動完整的 Client，用 WireMock 模擬 Spring_Eureka_Provider。
 * 測試不啟動 Eureka：改用 Spring Cloud 內建的 SimpleDiscoveryClient，把服務名稱 service-provider 指到 WireMock，
 * 因此「用服務名稱找到實例 → LoadBalancer 挑選 → 送出請求」的流程仍然完整經過。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class BookControllerTest {

	@RegisterExtension
	static WireMockExtension provider = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void discovery(DynamicPropertyRegistry registry) {
		registry.add("eureka.client.enabled", () -> "false");
		registry.add("spring.cloud.discovery.client.simple.instances.service-provider[0].uri", provider::baseUrl);
	}

	private static final String JAVA_BOOK = """
			{"id": 1, "isbn": 12345, "title": "Java", "author": "Tom", "year": 2020, "publisher": "OReilly", "cost": 450.5}
			""";

	private static final String NOT_FOUND_PROBLEM = """
			{"type": "about:blank", "title": "Not Found", "status": 404, "detail": "Book not found: id=999"}
			""";

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void hello_shouldReturnProviderInstanceId() {
		provider.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello from provider-1")));

		assertThat(restTemplate.getForObject("/api/hello", String.class)).isEqualTo("Hello from provider-1");
	}

	@Test
	void findAll_shouldReturnTypedBooks_whenProviderReturnsBooks() {
		provider.stubFor(get("/api/books").willReturn(okJson("[" + JAVA_BOOK + "]")));

		ResponseEntity<BookResponse[]> response = restTemplate.getForEntity("/api/books", BookResponse[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody())
				.containsExactly(new BookResponse(1, 12345, "Java", "Tom", 2020, "OReilly", 450.5));
	}

	@Test
	void findById_shouldReturnBook_whenProviderFindsIt() {
		provider.stubFor(get("/api/books/1").willReturn(okJson(JAVA_BOOK)));

		ResponseEntity<BookResponse> response = restTemplate.getForEntity("/api/books/1", BookResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().title()).isEqualTo("Java");
	}

	// 修正前：Provider 的 404 在 Client 變成 500，查無資料的原因消失
	@Test
	void findById_shouldPassThroughNotFoundProblemDetail_whenProviderReturnsNotFound() {
		provider.stubFor(get("/api/books/999").willReturn(aResponse().withStatus(404)
				.withHeader("Content-Type", "application/problem+json").withBody(NOT_FOUND_PROBLEM)));

		ResponseEntity<String> response = restTemplate.getForEntity("/api/books/999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).contains("Book not found: id=999");
	}

	@Test
	void create_shouldForwardJsonBodyAndReturnCreatedWithLocation_whenProviderCreatesBook() {
		provider.stubFor(post("/api/books").willReturn(aResponse().withStatus(201)
				.withHeader("Content-Type", "application/json").withBody(JAVA_BOOK)));
		BookRequest request = new BookRequest(12345, "Java", "Tom", 2020, "OReilly", 450.5);

		ResponseEntity<BookResponse> response = restTemplate.postForEntity("/api/books", request,
				BookResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasPath("/api/books/1");
		provider.verify(postRequestedFor(urlEqualTo("/api/books")).withRequestBody(equalToJson("""
				{"isbn": 12345, "title": "Java", "author": "Tom", "year": 2020, "publisher": "OReilly", "cost": 450.5}
				""")));
	}

	// 欄位驗證只在 Provider：Provider 回 400，Client 原樣轉回
	@Test
	void create_shouldPassThroughBadRequest_whenProviderRejectsRequest() {
		provider.stubFor(post("/api/books").willReturn(aResponse().withStatus(400)
				.withHeader("Content-Type", "application/problem+json")
				.withBody("{\"status\": 400, \"title\": \"Bad Request\", \"detail\": \"Invalid request content.\"}")));

		ResponseEntity<String> response = restTemplate.postForEntity("/api/books",
				new BookRequest(null, "T", null, null, null, null), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("Invalid request content.");
	}

	@Test
	void update_shouldForwardPutToProvider_whenRequestGiven() {
		provider.stubFor(put("/api/books/1").willReturn(okJson(JAVA_BOOK)));
		BookRequest request = new BookRequest(12345, "Java", "Tom", 2020, "OReilly", 450.5);

		ResponseEntity<BookResponse> response = restTemplate.exchange("/api/books/1", HttpMethod.PUT,
				new HttpEntity<>(request), BookResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		provider.verify(putRequestedFor(urlEqualTo("/api/books/1")));
	}

	@Test
	void delete_shouldReturnNoContent_whenProviderDeletesBook() {
		provider.stubFor(delete("/api/books/1").willReturn(aResponse().withStatus(204)));

		ResponseEntity<Void> response = restTemplate.exchange("/api/books/1", HttpMethod.DELETE, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void delete_shouldPassThroughNotFound_whenProviderReturnsNotFound() {
		provider.stubFor(delete("/api/books/999").willReturn(aResponse().withStatus(404)
				.withHeader("Content-Type", "application/problem+json").withBody(NOT_FOUND_PROBLEM)));

		ResponseEntity<String> response = restTemplate.exchange("/api/books/999", HttpMethod.DELETE, null,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// Provider 本身出錯時回 502：問題在下游服務，而不是 Client
	@Test
	void findAll_shouldReturnBadGateway_whenProviderReturnsInternalError() {
		provider.stubFor(get("/api/books").willReturn(aResponse().withStatus(500)));

		ResponseEntity<String> response = restTemplate.getForEntity("/api/books", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// Provider 回 404 但沒有內容時，Client 補上標準的 ProblemDetail
	@Test
	void findById_shouldReturnProblemDetail_whenProviderReturnsEmptyNotFound() {
		provider.stubFor(get("/api/books/999").willReturn(aResponse().withStatus(404)));

		ResponseEntity<String> response = restTemplate.getForEntity("/api/books/999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// 依專案 URL 規則改為 /api/...，舊路徑不再提供
	@Test
	void legacyEndpoints_shouldReturnNotFound() {
		assertThat(restTemplate.getForEntity("/Hello", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForEntity("/findall", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
