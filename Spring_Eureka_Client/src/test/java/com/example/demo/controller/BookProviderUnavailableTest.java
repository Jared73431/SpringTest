package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Provider 無法使用時，Client 一律回 503。分成兩種情況：
 * 註冊中心裡沒有任何實例，以及有實例但連不上（例如已停止，但還沒從 Eureka 清單移除）。
 */
class BookProviderUnavailableTest {

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
			properties = "eureka.client.enabled=false")
	@AutoConfigureTestRestTemplate
	class NoInstanceRegistered {

		@Autowired
		private TestRestTemplate restTemplate;

		// LoadBalancer 找不到實例時，Spring Cloud 自行產生 503 回應
		@Test
		void findAll_shouldReturnServiceUnavailable_whenNoProviderInstance() {
			ResponseEntity<String> response = restTemplate.getForEntity("/api/books", String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
			assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		}
	}

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
			"eureka.client.enabled=false",
			// port 1 沒有服務在監聽，連線會被拒絕或逾時
			"spring.cloud.discovery.client.simple.instances.service-provider[0].uri=http://localhost:1" })
	@AutoConfigureTestRestTemplate
	class InstanceUnreachable {

		@Autowired
		private TestRestTemplate restTemplate;

		@Test
		void findAll_shouldReturnServiceUnavailable_whenProviderUnreachable() {
			ResponseEntity<String> response = restTemplate.getForEntity("/api/books", String.class);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
			assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		}
	}
}
