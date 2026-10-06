package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.RedisContainerConfiguration;

/**
 * Baseline：以 Testcontainers 的 Redis 鎖定目前 /redis/... API 的行為。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RedisContainerConfiguration.class)
class RedisControllerBaselineTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@BeforeEach
	void flushRedis() {
		stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	private HttpEntity<String> text(String body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		return new HttpEntity<>(body, headers);
	}

	@Test
	void string_shouldSetAndGetValue() {
		ResponseEntity<String> set = restTemplate.postForEntity("/redis/string/hello", text("World"), String.class);

		assertThat(set.getBody()).isEqualTo("設置成功: hello = World");
		assertThat(restTemplate.getForObject("/redis/string/hello", String.class)).isEqualTo("World");
	}

	@Test
	void stringWithExpire_shouldSetTtl() {
		restTemplate.postForEntity("/redis/string/temp/expire?value=v&seconds=100", null, String.class);

		Long ttl = restTemplate.getForObject("/redis/ttl/temp", Long.class);
		assertThat(ttl).isBetween(1L, 100L);
	}

	@Test
	void user_shouldStoreObjectAsJson() {
		restTemplate.postForEntity("/redis/user/1", Map.of("name", "Amy", "email", "amy@example.com", "age", 20),
				String.class);

		Map<?, ?> user = restTemplate.getForObject("/redis/user/1", Map.class);
		assertThat(user.get("name")).isEqualTo("Amy");
		assertThat(user.get("id")).isEqualTo(1);
	}

	// [Critical] 目前使用 LaissezFaireSubTypeValidator 的 default typing：Redis 中的 JSON 帶有任意類別名稱（@class）
	@Test
	void user_shouldBeStoredWithClassNameTypeInfo() {
		restTemplate.postForEntity("/redis/user/1", Map.of("name", "Amy"), String.class);

		assertThat(stringRedisTemplate.opsForValue().get("user:1")).contains("\"@class\":\"com.example.demo.entity.User\"");
	}

	@Test
	void hash_shouldSetAndGetFields() {
		restTemplate.postForEntity("/redis/hash/profile/name", text("Amy"), String.class);
		restTemplate.postForEntity("/redis/hash/profile/city", text("Taipei"), String.class);

		assertThat(restTemplate.getForObject("/redis/hash/profile/name", String.class)).isEqualTo("Amy");
		assertThat(restTemplate.getForObject("/redis/hash/profile", Map.class)).containsOnlyKeys("name", "city");
	}

	@Test
	void list_shouldPushAndPop() {
		restTemplate.postForEntity("/redis/list/todo/right", text("a"), String.class);
		restTemplate.postForEntity("/redis/list/todo/right", text("b"), String.class);
		restTemplate.postForEntity("/redis/list/todo/left", text("c"), String.class);

		assertThat(restTemplate.getForObject("/redis/list/todo", List.class)).containsExactly("c", "a", "b");
		ResponseEntity<String> popped = restTemplate.exchange("/redis/list/todo/left", HttpMethod.DELETE, null,
				String.class);
		assertThat(popped.getBody()).isEqualTo("c");
	}

	@Test
	void set_shouldAddAndCheckMembers() {
		restTemplate.postForEntity("/redis/set/tags", new String[] { "Java", "Redis" }, String.class);

		assertThat(restTemplate.getForObject("/redis/set/tags", List.class)).containsExactlyInAnyOrder("Java", "Redis");
		assertThat(restTemplate.getForObject("/redis/set/tags/contains/Java", Boolean.class)).isTrue();
	}

	@Test
	void zset_shouldKeepScoreOrder() {
		restTemplate.postForEntity("/redis/zset/board?value=Alice&score=100", null, String.class);
		restTemplate.postForEntity("/redis/zset/board?value=Bob&score=85", null, String.class);

		assertThat(restTemplate.getForObject("/redis/zset/board", List.class)).containsExactly("Bob", "Alice");
		assertThat(restTemplate.getForObject("/redis/zset/board/score/Alice", Double.class)).isEqualTo(100.0);
	}

	@Test
	void keyOperations_shouldExpireCheckAndDelete() {
		restTemplate.postForEntity("/redis/string/k", text("v"), String.class);

		restTemplate.postForEntity("/redis/expire/k?seconds=50", null, String.class);
		assertThat(restTemplate.getForObject("/redis/ttl/k", Long.class)).isBetween(1L, 50L);
		assertThat(restTemplate.getForObject("/redis/exists/k", Boolean.class)).isTrue();

		restTemplate.exchange("/redis/k", HttpMethod.DELETE, null, String.class);
		assertThat(restTemplate.getForObject("/redis/exists/k", Boolean.class)).isFalse();
	}

	@Test
	void initTestData_shouldCreateSampleKeys() {
		ResponseEntity<Map> response = restTemplate.postForEntity("/redis/init-test-data", null, Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsKeys("string", "user", "hash", "list", "set", "zset");
		assertThat(stringRedisTemplate.hasKey("leaderboard")).isTrue();
	}
}
