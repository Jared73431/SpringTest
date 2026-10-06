package com.example.demo.scenario;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 情境二：API 限流（固定時間窗）。每個呼叫端在時間窗內最多呼叫 maxRequests 次，超過回 429。
 *
 * 做法：key = rate-limit:{clientId}，每次請求 INCR；第一次（計數為 1）時設定 EXPIRE，時間窗結束後 key 自動消失、重新計算。
 *
 * 為什麼用 Lua script：如果在 Java 中分成兩個指令
 * <pre>
 * long count = INCR key
 * if (count == 1) EXPIRE key 60      ← 應用程式在這兩行之間當掉，key 就永遠沒有過期時間，這個呼叫端會被永久封鎖
 * </pre>
 * Redis 執行 Lua script 時不會穿插其他指令，INCR 與 EXPIRE 一起成功，沒有中間狀態。
 */
@Component
public class RateLimiter {

	@ConfigurationProperties("demo.rate-limit")
	public record Properties(@DefaultValue("5") long maxRequests, @DefaultValue("60s") Duration window) {
	}

	/** allowed：是否允許；remaining：剩餘次數；retryAfterSeconds：多久後可以再呼叫 */
	public record Result(boolean allowed, long remaining, long retryAfterSeconds) {
	}

	private static final RedisScript<Long> INCREMENT_WITH_EXPIRE = RedisScript.of("""
			local count = redis.call('INCR', KEYS[1])
			if count == 1 then
			    redis.call('EXPIRE', KEYS[1], ARGV[1])
			end
			return count
			""", Long.class);

	private final StringRedisTemplate redis;

	private final Properties properties;

	public RateLimiter(StringRedisTemplate redis, Properties properties) {
		this.redis = redis;
		this.properties = properties;
	}

	public Result tryAcquire(String clientId) {
		String key = "rate-limit:" + clientId;
		long count = redis.execute(INCREMENT_WITH_EXPIRE, List.of(key),
				String.valueOf(properties.window().toSeconds()));
		long remaining = Math.max(0, properties.maxRequests() - count);
		Long ttl = redis.getExpire(key);
		return new Result(count <= properties.maxRequests(), remaining, ttl == null || ttl < 0 ? 0 : ttl);
	}
}
