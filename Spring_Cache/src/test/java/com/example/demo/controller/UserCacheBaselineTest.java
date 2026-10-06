package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

/**
 * Baseline：以 Testcontainers 的 PostgreSQL 與 Redis 鎖定目前的快取行為（包含 Bug）。
 * 每個測試前清空資料表與 Redis。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class UserCacheBaselineTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void reset() {
		userRepository.deleteAll();
		redis.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	private Map<?, ?> create(String name, String email) {
		return restTemplate.postForObject("/api/users", Map.of("name", name, "email", email, "age", 20), Map.class);
	}

	private Long idOf(Map<?, ?> user) {
		return ((Number) user.get("id")).longValue();
	}

	@Test
	void create_shouldReturnOk() {
		ResponseEntity<Map> response = restTemplate.postForEntity("/api/users",
				Map.of("name", "Amy", "email", "amy@example.com", "age", 20), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("id")).isNotNull();
	}

	// @Cacheable：第一次查詢後寫入快取，之後直接回傳快取（即使資料庫已被其他方式修改）
	@Test
	void findById_shouldServeFromCache_afterFirstCall() {
		Long id = idOf(create("Amy", "amy@example.com"));
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		User user = userRepository.findById(id).orElseThrow();
		user.setName("Changed In DB");
		userRepository.save(user);

		assertThat(restTemplate.getForObject("/api/users/{id}", Map.class, id).get("name")).isEqualTo("Amy");
	}

	// 自訂 CacheManager 蓋掉了 spring.cache.redis.key-prefix=app-cache::，前綴沒有生效
	@Test
	void cacheKey_shouldNotUseConfiguredPrefix() {
		Long id = idOf(create("Amy", "amy@example.com"));
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		assertThat(redis.hasKey("users::" + id)).isTrue();
		assertThat(redis.hasKey("app-cache::users::" + id)).isFalse();
	}

	@Test
	void update_shouldRefreshIdCache() {
		Long id = idOf(create("Amy", "amy@example.com"));
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		restTemplate.exchange("/api/users/{id}", HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", "Amy Lin", "email", "amy@example.com", "age", 21)), Map.class, id);

		assertThat(restTemplate.getForObject("/api/users/{id}", Map.class, id).get("name")).isEqualTo("Amy Lin");
	}

	// [Potential Bug] 修改後，email 快取沒有更新：用 email 查詢仍拿到舊資料
	@Test
	void findByEmail_shouldReturnStaleData_afterUpdate() {
		Long id = idOf(create("Amy", "amy@example.com"));
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");

		restTemplate.exchange("/api/users/{id}", HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", "Amy Lin", "email", "amy@example.com", "age", 21)), Map.class, id);

		assertThat(restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com").get("name"))
				.isEqualTo("Amy");
	}

	// [Potential Bug] 新增後，allUsers 快取沒有清除：列表仍是舊的
	@Test
	void findAll_shouldReturnStaleList_afterCreate() {
		create("Amy", "amy@example.com");
		assertThat(restTemplate.getForObject("/api/users", List.class)).hasSize(1);

		create("Ben", "ben@example.com");

		assertThat(restTemplate.getForObject("/api/users", List.class)).hasSize(1);
	}

	// [Potential Bug] 刪除後，email 快取與列表快取仍保留已刪除的使用者
	@Test
	void delete_shouldLeaveStaleEmailAndListCache() {
		Long id = idOf(create("Amy", "amy@example.com"));
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");
		restTemplate.getForObject("/api/users", List.class);

		restTemplate.delete("/api/users/{id}", id);

		assertThat(restTemplate.getForEntity("/api/users/email/{email}", Map.class, "amy@example.com")
				.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(restTemplate.getForObject("/api/users", List.class)).hasSize(1);
	}

	// [Potential Bug] 查無資料時回傳 null，但 CacheManager 設定了 disableCachingNullValues()：不是 404
	@Test
	void findById_shouldNotReturnNotFound_whenUserMissing() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/users/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	// [Critical] 任何人都能寫入任意 key，包括快取使用的 key（快取污染）
	@Test
	void customCache_shouldAllowOverwritingCacheEntries() {
		Long id = idOf(create("Amy", "amy@example.com"));
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		restTemplate.postForEntity("/api/users/cache/users::" + id, new HttpEntity<>("hacked", headers), String.class);

		assertThat(restTemplate.getForEntity("/api/users/{id}", String.class, id).getStatusCode())
				.isNotEqualTo(HttpStatus.OK);
	}

	@Test
	void clearCache_shouldEvictIdCache() {
		Long id = idOf(create("Amy", "amy@example.com"));
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		restTemplate.postForEntity("/api/users/cache/clear", null, String.class);

		assertThat(redis.hasKey("users::" + id)).isFalse();
	}
}
