package com.example.demo.scenario;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 三個實務情境的 API：排行榜、限流、分散式鎖。
 */
@RestController
public class ScenarioController {

	public record ScoreRequest(@NotBlank String player, @NotNull Double points) {
	}

	public record LockRequest(@Positive long ttlSeconds) {
	}

	private final LeaderboardService leaderboard;

	private final RateLimiter rateLimiter;

	private final DistributedLock lock;

	public ScenarioController(LeaderboardService leaderboard, RateLimiter rateLimiter, DistributedLock lock) {
		this.leaderboard = leaderboard;
		this.rateLimiter = rateLimiter;
		this.lock = lock;
	}

	// ===== 排行榜 =====

	@PostMapping("/api/redis/leaderboards/{board}/scores")
	public LeaderboardService.Entry addPoints(@PathVariable String board, @Valid @RequestBody ScoreRequest request) {
		return leaderboard.addPoints(board, request.player(), request.points());
	}

	@GetMapping("/api/redis/leaderboards/{board}")
	public List<LeaderboardService.Entry> top(@PathVariable String board,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int top) {
		return leaderboard.top(board, top);
	}

	@GetMapping("/api/redis/leaderboards/{board}/players/{player}")
	public LeaderboardService.Entry find(@PathVariable String board, @PathVariable String player) {
		return leaderboard.find(board, player);
	}

	// ===== 限流 =====

	/** 用 X-Client-Id 區分呼叫端；超過次數回 429，並以 Retry-After 告知幾秒後可以再試 */
	@GetMapping("/api/redis/rate-limited")
	public ResponseEntity<?> rateLimited(@RequestHeader(name = "X-Client-Id", defaultValue = "anonymous") String clientId) {
		RateLimiter.Result result = rateLimiter.tryAcquire(clientId);
		if (!result.allowed()) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
					"Rate limit exceeded for client '" + clientId + "'");
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.header(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()))
					.body(problem);
		}
		return ResponseEntity.ok()
				.header("X-RateLimit-Remaining", String.valueOf(result.remaining()))
				.body(Map.of("message", "OK", "remaining", result.remaining()));
	}

	// ===== 分散式鎖 =====

	/** 取得鎖：成功回 201 與 token；已被他人持有回 409 */
	@PostMapping("/api/redis/locks/{name}")
	public ResponseEntity<?> lock(@PathVariable String name, @Valid @RequestBody LockRequest request) {
		return lock.tryLock(name, Duration.ofSeconds(request.ttlSeconds()))
				.<ResponseEntity<?>>map(token -> ResponseEntity.created(URI.create("/api/redis/locks/" + name))
						.body(Map.of("name", name, "token", token)))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail
						.forStatusAndDetail(HttpStatus.CONFLICT, "Lock '" + name + "' is held by someone else")));
	}

	/** 釋放鎖：必須帶上取得時的 token（X-Lock-Token）；token 不符或鎖已過期回 409 */
	@DeleteMapping("/api/redis/locks/{name}")
	public ResponseEntity<?> unlock(@PathVariable String name, @RequestHeader("X-Lock-Token") String token) {
		if (lock.unlock(name, token)) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Lock '" + name + "' is not held with this token (expired or owned by someone else)"));
	}
}
