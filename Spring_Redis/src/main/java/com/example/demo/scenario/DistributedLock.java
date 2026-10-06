package com.example.demo.scenario;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 情境三：分散式鎖。多台應用程式要確保「同一時間只有一個人在做某件事」（例如同一張訂單只處理一次）時使用。
 *
 * <pre>
 * 取得鎖：SET lock:{name} {token} NX EX {ttl}
 *         NX：key 不存在時才設定（已經有人持有就失敗）
 *         EX：設定過期時間，持有者當掉時鎖會自動釋放，不會永久卡住
 * 釋放鎖：只有 value 等於自己的 token 時才刪除（Lua script，見 RELEASE）
 * </pre>
 *
 * 為什麼每次取得鎖都產生一個隨機 token：
 * A 取得鎖後處理太久，鎖過期被 B 取得；A 處理完直接 DEL，會把 <b>B 的鎖</b>刪掉。
 * 比對 token 可以確保「只刪除自己的鎖」；而「比對 + 刪除」必須是原子操作，所以用 Lua script。
 *
 * 實務上建議使用 Redisson：支援自動續期（watchdog）、可重入、公平鎖等，這裡示範的是原理。
 */
@Component
public class DistributedLock {

	private static final RedisScript<Long> RELEASE = RedisScript.of("""
			if redis.call('GET', KEYS[1]) == ARGV[1] then
			    return redis.call('DEL', KEYS[1])
			end
			return 0
			""", Long.class);

	private final StringRedisTemplate redis;

	public DistributedLock(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/** 取得成功時回傳 token（釋放時需要）；已被他人持有時回傳 empty */
	public Optional<String> tryLock(String name, Duration ttl) {
		String token = UUID.randomUUID().toString();
		Boolean acquired = redis.opsForValue().setIfAbsent(key(name), token, ttl);
		return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
	}

	/** token 相符才釋放；回傳是否真的釋放了 */
	public boolean unlock(String name, String token) {
		Long deleted = redis.execute(RELEASE, List.of(key(name)), token);
		return deleted != null && deleted == 1;
	}

	private static String key(String name) {
		return "lock:" + name;
	}
}
