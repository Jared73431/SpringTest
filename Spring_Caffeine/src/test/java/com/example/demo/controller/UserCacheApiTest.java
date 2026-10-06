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
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.config.CacheNames;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

/**
 * 快取行為的整合測試，案例與 Spring_Cache 相同：同一套註解，換成 Caffeine 後行為必須一致。
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
	private CacheManager cacheManager;

	@BeforeEach
	void reset() {
		userRepository.deleteAll();
		List.of(CacheNames.USERS, CacheNames.USERS_BY_EMAIL, CacheNames.ALL_USERS)
				.forEach(name -> cacheManager.getCache(name).invalidate());
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

	// 查無資料：不寫入快取，回 404
	@Test
	void findById_shouldReturnNotFoundAndNotCache_whenUserMissing() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/users/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(cacheManager.getCache(CacheNames.USERS).get(999999L)).isNull();
	}

	// ===== 快取一致性 =====

	@Test
	void update_shouldRefreshIdCacheAndEvictEmailCache() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");

		update(id, "Amy Lin", "amy@example.com");

		assertThat(restTemplate.getForObject("/api/users/{id}", Map.class, id).get("name")).isEqualTo("Amy Lin");
		assertThat(restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com").get("name"))
				.isEqualTo("Amy Lin");
	}

	@Test
	void update_shouldReturnNotFound_whenUserMissing() {
		ResponseEntity<String> response = restTemplate.exchange("/api/users/999999", HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", "X", "email", "x@example.com", "age", 1)), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void create_shouldEvictAllUsersCache() {
		create("Amy", "amy@example.com");
		assertThat(restTemplate.getForObject("/api/users", List.class)).hasSize(1);

		create("Ben", "ben@example.com");

		assertThat(restTemplate.getForObject("/api/users", List.class)).hasSize(2);
	}

	@Test
	void delete_shouldEvictAllRelatedCaches() {
		Long id = create("Amy", "amy@example.com");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");
		restTemplate.getForObject("/api/users", List.class);

		restTemplate.delete("/api/users/{id}", id);

		assertThat(restTemplate.getForEntity("/api/users/{id}", String.class, id).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForEntity("/api/users/email/{email}", String.class, "amy@example.com")
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForObject("/api/users", List.class)).isEmpty();
	}

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
		assertThat(restTemplate.postForEntity("/api/users", Map.of("name", "Amy", "email", "not-an-email"),
				String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
