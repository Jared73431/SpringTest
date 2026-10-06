package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.RedisContainerConfiguration;

/**
 * 五種資料結構與 key 操作的 API 測試，Redis 由 Testcontainers 啟動，每個測試前清空。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(RedisContainerConfiguration.class)
class RedisDataApiTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@BeforeEach
	void flushRedis() {
		stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	private ResponseEntity<Map> put(String url, Object body) {
		return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body), Map.class);
	}

	// ===== String =====

	@Test
	void string_shouldSetAndGetValueWithoutTtl() {
		put("/api/redis/strings/hello", Map.of("value", "World"));

		Map<?, ?> body = restTemplate.getForObject("/api/redis/strings/hello", Map.class);
		assertThat(body.get("value")).isEqualTo("World");
		assertThat(body.get("ttlSeconds")).isNull();
	}

	@Test
	void string_shouldExpire_whenTtlGiven() {
		ResponseEntity<Map> response = put("/api/redis/strings/otp", Map.of("value", "123456", "ttlSeconds", 60));

		assertThat(((Number) response.getBody().get("ttlSeconds")).longValue()).isBetween(1L, 60L);
	}

	@Test
	void string_shouldReturnNotFound_whenKeyMissing() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/redis/strings/missing", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void string_shouldReturnBadRequest_whenValueMissing() {
		ResponseEntity<Map> response = put("/api/redis/strings/hello", Map.of());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	// ===== User 物件 =====

	@Test
	void user_shouldStoreTypedJsonAndKeepCreateTime() {
		put("/api/redis/users/1", Map.of("name", "Amy", "email", "amy@example.com", "age", 20));
		Map<?, ?> first = restTemplate.getForObject("/api/redis/users/1", Map.class);

		put("/api/redis/users/1", Map.of("name", "Amy Lin", "email", "amy@example.com", "age", 21));
		Map<?, ?> updated = restTemplate.getForObject("/api/redis/users/1", Map.class);

		assertThat(updated.get("name")).isEqualTo("Amy Lin");
		assertThat(updated.get("createTime")).isEqualTo(first.get("createTime"));
		// 安全性：明確指定型別，Redis 中只存放一般的 JSON，不帶類別名稱（@class）
		assertThat(stringRedisTemplate.opsForValue().get("user:1")).contains("\"name\":\"Amy Lin\"")
				.doesNotContain("@class");
	}

	@Test
	void user_shouldReturnNotFound_whenMissing() {
		assertThat(restTemplate.getForEntity("/api/redis/users/999", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	// ===== Hash =====

	@Test
	void hash_shouldSetAndGetFields() {
		put("/api/redis/hashes/profile/fields/name", Map.of("value", "Amy"));
		put("/api/redis/hashes/profile/fields/city", Map.of("value", "Taipei"));

		assertThat(restTemplate.getForObject("/api/redis/hashes/profile/fields/name", Map.class).get("value"))
				.isEqualTo("Amy");
		assertThat(restTemplate.getForObject("/api/redis/hashes/profile", Map.class))
				.isEqualTo(Map.of("name", "Amy", "city", "Taipei"));
		assertThat(restTemplate.getForEntity("/api/redis/hashes/profile/fields/age", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	// ===== List =====

	@Test
	void list_shouldPushBothEndsAndPop() {
		restTemplate.postForEntity("/api/redis/lists/todo/right", Map.of("value", "a"), List.class);
		restTemplate.postForEntity("/api/redis/lists/todo/right", Map.of("value", "b"), List.class);
		ResponseEntity<List> pushed = restTemplate.postForEntity("/api/redis/lists/todo/left", Map.of("value", "c"),
				List.class);

		assertThat(pushed.getBody()).containsExactly("c", "a", "b");
		ResponseEntity<Map> popped = restTemplate.exchange("/api/redis/lists/todo/right", HttpMethod.DELETE, null,
				Map.class);
		assertThat(popped.getBody().get("value")).isEqualTo("b");
	}

	@Test
	void list_shouldReturnNotFound_whenPoppingEmptyList() {
		assertThat(restTemplate.exchange("/api/redis/lists/empty/left", HttpMethod.DELETE, null, String.class)
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// ===== Set =====

	@Test
	void set_shouldIgnoreDuplicatesAndCheckMembership() {
		ResponseEntity<List> response = restTemplate.postForEntity("/api/redis/sets/tags",
				Map.of("members", List.of("Java", "Redis", "Java")), List.class);

		assertThat(response.getBody()).containsExactlyInAnyOrder("Java", "Redis");
		assertThat(restTemplate.getForObject("/api/redis/sets/tags/members/Java", Map.class).get("isMember"))
				.isEqualTo(true);
		assertThat(restTemplate.getForObject("/api/redis/sets/tags/members/Go", Map.class).get("isMember"))
				.isEqualTo(false);
	}

	// ===== ZSet =====

	@Test
	void zset_shouldReturnMembersOrderedByScore() {
		restTemplate.postForEntity("/api/redis/zsets/scores", Map.of("member", "Alice", "score", 100), List.class);
		ResponseEntity<List> response = restTemplate.postForEntity("/api/redis/zsets/scores",
				Map.of("member", "Bob", "score", 85), List.class);

		assertThat(response.getBody()).containsExactly(Map.of("member", "Bob", "score", 85.0),
				Map.of("member", "Alice", "score", 100.0));
		assertThat(restTemplate.getForObject("/api/redis/zsets/scores/members/Alice", Map.class).get("score"))
				.isEqualTo(100.0);
	}

	// ===== Key =====

	@Test
	void key_shouldSetTtlAndDelete() {
		put("/api/redis/strings/k", Map.of("value", "v"));

		ResponseEntity<Map> expired = put("/api/redis/keys/k/ttl", Map.of("seconds", 50));
		assertThat(((Number) expired.getBody().get("ttlSeconds")).longValue()).isBetween(1L, 50L);

		assertThat(restTemplate.exchange("/api/redis/keys/k", HttpMethod.DELETE, null, Void.class).getStatusCode())
				.isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(restTemplate.getForEntity("/api/redis/keys/k", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void key_shouldReturnNotFound_whenDeletingMissingKey() {
		assertThat(restTemplate.exchange("/api/redis/keys/missing", HttpMethod.DELETE, null, String.class)
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// 對 String 的 key 使用 Hash 操作：Redis 回 WRONGTYPE → 409
	@Test
	void wrongType_shouldReturnConflict() {
		put("/api/redis/strings/k", Map.of("value", "v"));

		ResponseEntity<String> response = restTemplate.getForEntity("/api/redis/hashes/k", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).contains("WRONGTYPE");
	}

	// ===== 其他 =====

	@Test
	void sampleData_shouldCreateKeys() {
		ResponseEntity<List> response = restTemplate.postForEntity("/api/redis/sample-data", null, List.class);

		assertThat(response.getBody()).contains("todo:list", "scores");
		assertThat(stringRedisTemplate.hasKey("scores")).isTrue();
	}

	// 依專案 URL 規則改為 /api/redis/...，舊路徑不再提供
	@Test
	void legacyEndpoints_shouldReturnNotFound() {
		assertThat(restTemplate.getForEntity("/redis/string/hello", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.postForEntity("/redis/init-test-data", null, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
