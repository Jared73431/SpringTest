package com.example.demo.lesson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.http.Fault;

/**
 * 第 2 課：Server 出錯時怎麼辦？
 *
 * <pre>
 * Server 回 4xx         → RestClientResponseException → 原樣轉回（狀態碼與 ProblemDetail 不變）
 * Server 回 5xx         → RestClientResponseException → 502 Bad Gateway
 * 連不上、讀取逾時       → ResourceAccessException    → 503 Service Unavailable
 * </pre>
 *
 * 和 Feign 的差別：Feign 要自己寫 ErrorDecoder 才拿得到狀態碼與回應內容；RestClient 預設就拋出帶有這些資訊的例外。
 *
 * <p>
 * 這個測試把讀取逾時縮短為 500ms，讓逾時的情境很快發生。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.http.serviceclient.book-server.read-timeout=500ms")
class L02_ErrorHandlingTest extends WireMockTestBase {

	@Test
	void server4xx_shouldBePassedThroughUnchanged() {
		server.stubFor(get("/api/books/999").willReturn(aResponse().withStatus(404)
				.withHeader("Content-Type", "application/problem+json").withBody(NOT_FOUND_PROBLEM)));

		var response = client().get().uri("/api/books/999").retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).contains("Book not found: id=999");
	}

	@Test
	void server5xx_shouldBecome502() {
		server.stubFor(get("/api/books").willReturn(aResponse().withStatus(500)));

		var response = client().get().uri("/api/books").retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(response.getBody()).contains("Book server error: status 500");
	}

	@Test
	void brokenConnection_shouldBecome503() {
		server.stubFor(get("/api/books").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		var response = client().get().uri("/api/books").retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getBody()).contains("Book server is unavailable");
	}

	// Server 2 秒才回應，超過讀取逾時（500ms）：不會一直卡住
	@Test
	void slowServer_shouldTimeOutAndBecome503() {
		server.stubFor(get("/api/books").willReturn(aResponse().withFixedDelay(2000).withBody("[]")));

		long start = System.currentTimeMillis();
		var response = client().get().uri("/api/books").retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(System.currentTimeMillis() - start).isLessThan(1900);
	}
}
