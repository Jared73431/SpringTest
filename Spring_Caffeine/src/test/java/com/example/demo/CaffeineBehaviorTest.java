package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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

import com.example.demo.config.CacheNames;
import com.example.demo.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

/**
 * Caffeine 特有的行為：容量上限淘汰、過期、命中統計、Actuator 指標。
 * 測試中把 users 上限設為 2 筆、usersByEmail 存活時間設為 1 秒，方便觀察。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"app.cache.users.maximum-size=2",
		"app.cache.users-by-email.expire-after-write=1s" })
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class CaffeineBehaviorTest {

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

	private Long create(String name) {
		Map<?, ?> body = restTemplate.postForObject("/api/users",
				Map.of("name", name, "email", name.toLowerCase() + "@example.com", "age", 20), Map.class);
		return ((Number) body.get("id")).longValue();
	}

	// 本機快取一定要有上限：超過 maximumSize 時，Caffeine 自動淘汰資料
	@Test
	void users_shouldNeverExceedMaximumSize() {
		Cache<Object, Object> users = CaffeineCaches.nativeCache(cacheManager, CacheNames.USERS);
		long evictionsBefore = users.stats().evictionCount();
		List<Long> ids = new ArrayList<>();
		for (String name : List.of("Amy", "Ben", "Cat", "Dan", "Eve")) {
			ids.add(create(name));
		}

		ids.forEach(id -> restTemplate.getForObject("/api/users/{id}", Map.class, id));
		// 淘汰是非同步進行的；cleanUp() 讓 Caffeine 立即完成待處理的維護工作，測試才能穩定判斷
		users.cleanUp();

		assertThat(users.estimatedSize()).isLessThanOrEqualTo(2);
		assertThat(users.stats().evictionCount() - evictionsBefore).isGreaterThanOrEqualTo(3);
	}

	// expireAfterWrite：寫入後超過存活時間就失效，下一次查詢會重新讀取資料庫
	@Test
	void usersByEmail_shouldExpireAfterWrite() throws InterruptedException {
		create("Amy");
		restTemplate.getForObject("/api/users/email/{email}", Map.class, "amy@example.com");
		assertThat(cacheManager.getCache(CacheNames.USERS_BY_EMAIL).get("amy@example.com")).isNotNull();

		Thread.sleep(1500);

		assertThat(cacheManager.getCache(CacheNames.USERS_BY_EMAIL).get("amy@example.com")).isNull();
	}

	// recordStats()：記錄命中與未命中次數
	@Test
	void stats_shouldCountHitsAndMisses() {
		Long id = create("Amy");
		Cache<Object, Object> users = CaffeineCaches.nativeCache(cacheManager, CacheNames.USERS);
		CacheStats before = users.stats();

		restTemplate.getForObject("/api/users/{id}", Map.class, id); // 未命中 → 查資料庫
		restTemplate.getForObject("/api/users/{id}", Map.class, id); // 命中
		restTemplate.getForObject("/api/users/{id}", Map.class, id); // 命中

		CacheStats delta = users.stats().minus(before);
		assertThat(delta.missCount()).isEqualTo(1);
		assertThat(delta.hitCount()).isEqualTo(2);
	}

	// Actuator：快取清單與 Micrometer 指標
	@Test
	void actuator_shouldExposeCachesAndHitMetrics() {
		Long id = create("Amy");
		restTemplate.getForObject("/api/users/{id}", Map.class, id);
		restTemplate.getForObject("/api/users/{id}", Map.class, id);

		Map<?, ?> caches = restTemplate.getForObject("/actuator/caches", Map.class);
		assertThat(caches.toString()).contains("users", "usersByEmail", "allUsers");

		Map<?, ?> hits = restTemplate.getForObject("/actuator/metrics/cache.gets?tag=name:users&tag=result:hit",
				Map.class);
		List<?> measurements = (List<?>) hits.get("measurements");
		double value = ((Number) ((Map<?, ?>) measurements.get(0)).get("value")).doubleValue();
		assertThat(value).isGreaterThanOrEqualTo(1);
	}
}
