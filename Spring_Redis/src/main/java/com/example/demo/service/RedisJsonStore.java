package com.example.demo.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * 以 JSON 存取任意型別的物件：只用 StringRedisTemplate，讀取時才指定型別。
 *
 * <pre>
 * User    → RedisTemplate&lt;String, User&gt;（RedisConfig）：一種型別一個 bean
 * Product → RedisJsonStore（本類別）：           一個元件處理所有型別
 * </pre>
 *
 * 物件只有兩三種時，一種型別一個 RedisTemplate 就夠了；種類變多時，不必為每種型別新增 bean。
 * 型別由<b>呼叫端</b>指定（get(key, Product.class)），不是由 Redis 中的資料決定，
 * 所以 JSON 不需要帶 @class，也不會建立呼叫端沒有要求的類別。
 * 需要讀取 List&lt;Product&gt; 這類泛型型別時，可以再加一個接收 TypeReference 的方法。
 */
@Component
public class RedisJsonStore {

	private final StringRedisTemplate redis;

	private final JsonMapper jsonMapper;

	// 使用 Spring Boot 已設定好的 JsonMapper：與 API 回應的 JSON 格式一致，java.time 也已支援
	public RedisJsonStore(StringRedisTemplate redis, JsonMapper jsonMapper) {
		this.redis = redis;
		this.jsonMapper = jsonMapper;
	}

	/** ttl 為 null 表示不過期；Jackson 3 的例外都是 unchecked，不需要 try/catch */
	public void set(String key, Object value, Duration ttl) {
		String json = jsonMapper.writeValueAsString(value);
		if (ttl == null) {
			redis.opsForValue().set(key, json);
		} else {
			redis.opsForValue().set(key, json, ttl);
		}
	}

	public <T> Optional<T> get(String key, Class<T> type) {
		return Optional.ofNullable(redis.opsForValue().get(key))
				.map(json -> jsonMapper.readValue(json, type));
	}
}
