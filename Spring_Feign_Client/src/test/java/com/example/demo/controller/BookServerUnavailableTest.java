package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Server 沒有啟動（連不上）時的行為。book-server.url 指向沒有服務在監聽的 port 1，連線會被拒絕或逾時。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "book-server.url=http://localhost:1")
@AutoConfigureTestRestTemplate
class BookServerUnavailableTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void findAll_shouldReturnServiceUnavailable_whenServerIsDown() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/books", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}
}
