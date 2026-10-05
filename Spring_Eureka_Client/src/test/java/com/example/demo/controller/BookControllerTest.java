package com.example.demo.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Baseline：不啟動 Eureka，改用 Spring Cloud 內建的 SimpleDiscoveryClient，
 * 把服務名稱 service-provider 指到 WireMock（假的 Provider），鎖定目前 Client 的行為。
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

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void hello_shouldReturnProviderResponse() {
		provider.stubFor(get("/hello").willReturn(aResponse().withBody("Hello")));

		assertThat(restTemplate.getForObject("/Hello", String.class)).isEqualTo("Hello");
	}

	@Test
	void findall_shouldReturnProviderBooks() {
		provider.stubFor(get("/findall").willReturn(aResponse().withHeader("Content-Type", "application/json")
				.withBody("[{\"id\":1,\"isbn\":12345,\"title\":\"Java\"}]")));

		ResponseEntity<String> response = restTemplate.getForEntity("/findall", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"title\":\"Java\"");
	}

	// [Potential Bug] Provider 回 4xx 時，Feign 拋出 FeignException，Client 一律回 500，原本的狀態碼與原因消失
	@Test
	void findall_shouldReturnInternalServerError_whenProviderReturnsNotFound() {
		provider.stubFor(get("/findall").willReturn(aResponse().withStatus(404)));

		ResponseEntity<String> response = restTemplate.getForEntity("/findall", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
