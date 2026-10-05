package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.model.Post;

import mockwebserver3.RecordedRequest;

/**
 * /api/posts（同步）的測試：繼承「查詢一筆」的共用案例，另外測試其他 CRUD 與 interceptor。
 */
class SyncPostApiTest extends PostApiContractTest {

	@Override
	String basePath() {
		return "/api/posts";
	}

	@Test
	void findAll_shouldReturnTypedPosts() {
		stub("GET /posts", json(200, "[" + POST_1 + "]"));

		ResponseEntity<Post[]> response = restTemplate.getForEntity("/api/posts", Post[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsExactly(new Post(1L, 1L, "t1", "b1"));
	}

	@Test
	void create_shouldSendJsonAndReturnCreatedWithLocation() throws InterruptedException {
		stub("POST /posts", json(201, "{\"id\": 101, \"userId\": 1, \"title\": \"new\", \"body\": \"b\"}"));

		ResponseEntity<Post> response = restTemplate.postForEntity("/api/posts", Post.of(1L, "new", "b"), Post.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasPath("/api/posts/101");
		RecordedRequest request = SERVER.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getHeaders().get("Content-Type")).startsWith("application/json");
		assertThat(request.getBody().utf8()).contains("\"title\":\"new\"");
	}

	@Test
	void update_shouldSendPut() throws InterruptedException {
		stub("PUT /posts/1", json(200, POST_1));

		ResponseEntity<Post> response = restTemplate.exchange("/api/posts/1", HttpMethod.PUT,
				new HttpEntity<>(new Post(1L, 1L, "t1", "b1")), Post.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(SERVER.takeRequest(1, TimeUnit.SECONDS).getMethod()).isEqualTo("PUT");
	}

	@Test
	void delete_shouldReturnNoContent() {
		stub("DELETE /posts/1", json(200, "{}"));

		ResponseEntity<Void> response = restTemplate.exchange("/api/posts/1", HttpMethod.DELETE, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	// UserAgentInterceptor：每個請求自動加上 User-Agent
	@Test
	void everyRequest_shouldCarryUserAgentFromInterceptor() throws InterruptedException {
		stub("GET /posts/1", json(200, POST_1));

		restTemplate.getForEntity("/api/posts/1", Post.class);

		assertThat(SERVER.takeRequest(1, TimeUnit.SECONDS).getHeaders().get("User-Agent"))
				.isEqualTo("Spring-Okhttp-Demo/1.0");
	}
}
