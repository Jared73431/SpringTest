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

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

/**
 * 快取行為的整合測試：PostgreSQL 與 Redis 由 Testcontainers 啟動，每個測試前清空。
 *
 * 判斷「資料來自快取」的方式：查詢一次後<b>繞過 Service 直接修改資料庫</b>，
 * 如果再次查詢仍是舊值，代表結果來自快取。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class UserCacheApiTest {

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

	private Long create(String name, String email) {
		Map<?, ?> body = restTemplate.postForObject("/api/users", Map.of("name", name, "email", email, "age", 20),
				Map.class);
		return ((Number) body.get("id")).longValue();
	}

	private void update(Long id, String name, String email) {
		restTemplate.exchange("/api/users/{id}", HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", name, "email", email, "age", 21)), Map.class, id);
	}

	/** 繞過 Service 直接改資料庫：快取不會知道 */
	private void changeNameInDatabaseOnly(Long id, String name) {
		User user = userRepository.findById(id).orElseThrow();
		user.setName(name);
		userRepository.save(user);
	}

	// ===== @Cacheable =====

	@Test
	void findById_shouldServeFromCache_afterFirstCall() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		changeNameInDatabaseOnly(id, "Changed In DB");

		assertThat(restTemplate.getForObject("/api/users/{id}", Map.class, id).get("name")).isEqualTo("Amy");
	}

	@Test
	void cachedValues_shouldUsePrefixAndPlainJson() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);
		restTemplate.getForObject("/api/users", List.class);

		// 修正前 key-prefix 設定沒有生效（自訂 CacheManager 蓋掉了 spring.cache.redis.*）
		assertThat(redis.opsForValue().get("app-cache::users::" + id)).contains("\"name\":\"Amy\"")
				.doesNotContain("@class");
		assertThat(redis.opsForValue().get("app-cache::allUsers::all")).contains("\"name\":\"Amy\"");
		assertThat(redis.getExpire("app-cache::users::" + id)).isBetween(1L, 600L);
	}

	// ===== 查無資料 =====

	// 修正前：回傳 null + disableCachingNullValues() → IllegalArgumentException → 500
	@Test
	void findById_shouldReturnNotFoundAndNotCache_whenUserMissing() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/users/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(redis.hasKey("app-cache::users::999999")).isFalse();
	}

	@Test
	void findByEmail_shouldReturnNotFound_whenUserMissing() {
		assertThat(restTemplate.getForEntity("/api/users/email/{email}", String.class, "nobody@example.com")
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// ===== 快取一致性：修改 =====

	@Test
	void update_shouldRefreshIdCache() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		update(id, "Amy Lin", "amy@example.com");

		assertThat(restTemplate.getForObject("/api/users/{id}", Map.class, id).get("name")).isEqualTo("Amy Lin");
	}

	// 修正前：email 快取沒有更新，用 email 查詢仍拿到舊資料
	@Test
	void update_shouldEvictEmailCache() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");

		update(id, "Amy Lin", "amy@example.com");

		assertThat(restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com").get("name"))
				.isEqualTo("Amy Lin");
	}

	// email 被修改後，舊 email 不應該再查得到
	@Test
	void update_shouldMakeOldEmailUnreachable_whenEmailChanged() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");

		update(id, "Amy", "amy.lin@example.com");

		assertThat(restTemplate.getForEntity("/api/users/email/{email}", String.class, "amy@example.com")
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void update_shouldReturnNotFound_whenUserMissing() {
		ResponseEntity<String> response = restTemplate.exchange("/api/users/999999", HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", "X", "email", "x@example.com", "age", 1)), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(userRepository.count()).isZero();
	}

	// ===== 快取一致性：新增、刪除 =====

	// 修正前：allUsers 快取從不清除，新增後列表仍是舊的
	@Test
	void create_shouldEvictAllUsersCache() {
		create("Amy", "amy@example.com");
		assertThat(restTemplate.getForObject("/api/users", List.class)).hasSize(1);

		create("Ben", "ben@example.com");

		assertThat(restTemplate.getForObject("/api/users", List.class)).hasSize(2);
	}

	// 修正前：刪除後 email 快取與列表快取仍保留已刪除的使用者
	@Test
	void delete_shouldEvictAllRelatedCaches() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");
		restTemplate.getForObject("/api/users", List.class);

		assertThat(restTemplate.exchange("/api/users/{id}", HttpMethod.DELETE, null, Void.class, id)
				.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertThat(restTemplate.getForEntity("/api/users/{id}", String.class, id).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForEntity("/api/users/email/{email}", String.class, "amy@example.com")
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForObject("/api/users", List.class)).isEmpty();
	}

	@Test
	void delete_shouldReturnNotFound_whenUserMissing() {
		assertThat(restTemplate.exchange("/api/users/999999", HttpMethod.DELETE, null, String.class)
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// ===== 清除快取 =====

	@Test
	void clearCaches_shouldMakeNextReadHitDatabase() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);
		changeNameInDatabaseOnly(id, "Changed In DB");

		restTemplate.exchange("/api/users/cache", HttpMethod.DELETE, null, Void.class);

		assertThat(restTemplate.getForObject("/api/users/{id}", Map.class, id).get("name")).isEqualTo("Changed In DB");
	}

	// ===== REST =====

	@Test
	void create_shouldReturnCreatedWithLocation() {
		ResponseEntity<Map> response = restTemplate.postForEntity("/api/users",
				Map.of("name", "Amy", "email", "amy@example.com", "age", 20), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasPath("/api/users/" + response.getBody().get("id"));
	}

	@Test
	void create_shouldReturnBadRequest_whenEmailInvalid() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/users",
				Map.of("name", "Amy", "email", "not-an-email"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(userRepository.count()).isZero();
	}

	// 修正前可以讀寫任意 Redis key（快取污染），已移除；舊的清除快取路徑也改為 DELETE /api/users/cache
	@Test
	void legacyCacheEndpoints_shouldBeRemoved() {
		assertThat(restTemplate.postForEntity("/api/users/cache/users::1", "hacked", String.class).getStatusCode()
				.is4xxClientError()).isTrue();
		assertThat(restTemplate.postForEntity("/api/users/cache/clear", null, String.class).getStatusCode()
				.is4xxClientError()).isTrue();
	}
}
