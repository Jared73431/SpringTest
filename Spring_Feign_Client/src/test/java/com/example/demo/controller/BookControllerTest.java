package com.example.demo.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Baseline：用 WireMock 模擬 Feign Server，鎖定目前 Client 的行為。
 * Server 網址目前寫死在 @FeignClient(url = "http://localhost:8082")，因此 WireMock 只能固定使用 8082。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookControllerTest {

	@RegisterExtension
	static WireMockExtension server = WireMockExtension.newInstance().options(wireMockConfig().port(8082)).build();

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void hello_shouldReturnServerResponse() {
		server.stubFor(get("/hello").willReturn(aResponse().withBody("Hello")));

		assertThat(restTemplate.getForObject("/HelloWorld", String.class)).isEqualTo("Hello");
	}

	@Test
	void findAllBook_shouldReturnServerBooks() {
		server.stubFor(get("/findall").willReturn(aResponse().withHeader("Content-Type", "application/json")
				.withBody("[{\"id\":1,\"isbn\":12345,\"title\":\"Java\"}]")));

		ResponseEntity<String> response = restTemplate.getForEntity("/findAllBook", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"title\":\"Java\"");
	}

	@Test
	void saveBook_shouldForwardQueryParamsToServer() {
		server.stubFor(post(urlPathEqualTo("/saveBook")).willReturn(aResponse().withStatus(200)));

		ResponseEntity<String> response = restTemplate.postForEntity(
				"/saveBook?ISBN=12345&title=Java&author=Tom&year=2020&publisher=OReilly&cost=450.5", null,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		server.verify(postRequestedFor(urlPathEqualTo("/saveBook")).withQueryParam("ISBN", equalTo("12345"))
				.withQueryParam("title", equalTo("Java")).withQueryParam("cost", equalTo("450.5")));
	}

	// [Potential Bug] Server 回 4xx 時，Feign 拋出 FeignException，Client 一律回 500，原本的狀態碼與原因消失
	@Test
	void findAllBook_shouldReturnInternalServerError_whenServerReturnsNotFound() {
		server.stubFor(get("/findall").willReturn(aResponse().withStatus(404)));

		ResponseEntity<String> response = restTemplate.getForEntity("/findAllBook", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
