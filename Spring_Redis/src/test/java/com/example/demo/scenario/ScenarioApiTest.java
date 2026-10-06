package com.example.demo.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.RedisContainerConfiguration;

/**
 * 排行榜、限流、分散式鎖三個情境的 API 測試。限流上限在測試中設為 3 次，方便驗證。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "demo.rate-limit.max-requests=3", "demo.rate-limit.window=30s" })
@AutoConfigureTestRestTemplate
@Import(RedisContainerConfiguration.class)
class ScenarioApiTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private DistributedLock distributedLock;

	@BeforeEach
	void flushRedis() {
		stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	// ===== 排行榜 =====

	@Test
	void leaderboard_shouldRankPlayersByAccumulatedPoints() {
		addPoints("Alice", 50);
		addPoints("Bob", 80);
		Map alice = addPoints("Alice", 40); // Alice 累計 90，超過 Bob

		assertThat(alice).containsEntry("rank", 1).containsEntry("score", 90.0);
		List top = restTemplate.getForObject("/api/redis/leaderboards/game?top=2", List.class);
		assertThat(top).containsExactly(Map.of("rank", 1, "player", "Alice", "score", 90.0),
				Map.of("rank", 2, "player", "Bob", "score", 80.0));
		assertThat(restTemplate.getForObject("/api/redis/leaderboards/game/players/Bob", Map.class).get("rank"))
				.isEqualTo(2);
	}

	@Test
	void leaderboard_shouldReturnNotFound_whenPlayerMissing() {
		assertThat(restTemplate.getForEntity("/api/redis/leaderboards/game/players/Nobody", String.class)
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private Map addPoints(String player, double points) {
		return restTemplate.postForObject("/api/redis/leaderboards/game/scores",
				Map.of("player", player, "points", points), Map.class);
	}

	// ===== 限流 =====

	@Test
	void rateLimit_shouldRejectWithRetryAfter_whenLimitExceeded() {
		for (int i = 0; i < 3; i++) {
			assertThat(callRateLimited("client-a").getStatusCode()).isEqualTo(HttpStatus.OK);
		}

		ResponseEntity<String> rejected = callRateLimited("client-a");

		assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(Long.parseLong(rejected.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))).isBetween(1L, 30L);
		// 不同的呼叫端各自計算
		assertThat(callRateLimited("client-b").getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void rateLimit_shouldSetExpiryOnCounterKey() {
		callRateLimited("client-a");

		assertThat(stringRedisTemplate.getExpire("rate-limit:client-a")).isBetween(1L, 30L);
	}

	private ResponseEntity<String> callRateLimited(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return restTemplate.exchange("/api/redis/rate-limited", HttpMethod.GET, new HttpEntity<>(headers),
				String.class);
	}

	// ===== 分散式鎖 =====

	@Test
	void lock_shouldBeExclusiveAndReleasableOnlyWithToken() {
		ResponseEntity<Map> first = restTemplate.postForEntity("/api/redis/locks/order-1", Map.of("ttlSeconds", 30),
				Map.class);
		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String token = (String) first.getBody().get("token");

		// 已被持有：其他人取得失敗
		assertThat(restTemplate.postForEntity("/api/redis/locks/order-1", Map.of("ttlSeconds", 30), String.class)
				.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		// 錯誤的 token 不能釋放別人的鎖
		assertThat(unlock("order-1", "wrong-token").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(stringRedisTemplate.hasKey("lock:order-1")).isTrue();

		assertThat(unlock("order-1", token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(stringRedisTemplate.hasKey("lock:order-1")).isFalse();
	}

	@Test
	void lock_shouldExpireAutomatically_whenHolderNeverReleases() {
		restTemplate.postForEntity("/api/redis/locks/order-2", Map.of("ttlSeconds", 30), Map.class);

		assertThat(stringRedisTemplate.getExpire("lock:order-2")).isBetween(1L, 30L);
	}

	// 10 個執行緒同時搶同一把鎖，只有 1 個成功
	@Test
	void lock_shouldGrantExactlyOneHolder_whenAcquiredConcurrently() throws Exception {
		int threads = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			Callable<Boolean> task = () -> {
				start.await();
				return distributedLock.tryLock("hot-item", java.time.Duration.ofSeconds(30)).isPresent();
			};
			results.add(executor.submit(task));
		}
		start.countDown();

		long acquired = 0;
		for (Future<Boolean> result : results) {
			if (result.get()) {
				acquired++;
			}
		}
		executor.shutdown();
		assertThat(acquired).isEqualTo(1);
	}

	private ResponseEntity<String> unlock(String name, String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Lock-Token", token);
		return restTemplate.exchange("/api/redis/locks/" + name, HttpMethod.DELETE, new HttpEntity<>(headers),
				String.class);
	}
}
