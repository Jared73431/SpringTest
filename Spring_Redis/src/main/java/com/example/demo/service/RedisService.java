package com.example.demo.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;

/**
 * Redis 五種資料結構的基本操作。
 * 純文字資料使用 StringRedisTemplate；User 物件使用專用的 RedisTemplate&lt;String, User&gt;（見 RedisConfig）。
 */
@Service
public class RedisService {

	private final StringRedisTemplate stringRedisTemplate;

	private final RedisTemplate<String, User> userRedisTemplate;

	public RedisService(StringRedisTemplate stringRedisTemplate, RedisTemplate<String, User> userRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.userRedisTemplate = userRedisTemplate;
	}

	// ========== String ==========

	public void setString(String key, String value) {
		stringRedisTemplate.opsForValue().set(key, value);
	}

	// Spring Data Redis 4：改用 Duration 版本（set(key, value, long, TimeUnit) 已 deprecated）
	public void setString(String key, String value, Duration timeout) {
		stringRedisTemplate.opsForValue().set(key, value, timeout);
	}

	public String getString(String key) {
		return stringRedisTemplate.opsForValue().get(key);
	}

	// ========== User 物件 ==========

	public void setUser(String key, User user) {
		userRedisTemplate.opsForValue().set(key, user);
	}

	public User getUser(String key) {
		return userRedisTemplate.opsForValue().get(key);
	}

	// ========== Hash ==========

	public void setHash(String key, String field, String value) {
		stringRedisTemplate.opsForHash().put(key, field, value);
	}

	public Object getHash(String key, String field) {
		return stringRedisTemplate.opsForHash().get(key, field);
	}

	public Map<Object, Object> getAllHash(String key) {
		return stringRedisTemplate.opsForHash().entries(key);
	}

	// ========== List ==========

	public void leftPushList(String key, String value) {
		stringRedisTemplate.opsForList().leftPush(key, value);
	}

	public void rightPushList(String key, String value) {
		stringRedisTemplate.opsForList().rightPush(key, value);
	}

	public String leftPopList(String key) {
		return stringRedisTemplate.opsForList().leftPop(key);
	}

	public String rightPopList(String key) {
		return stringRedisTemplate.opsForList().rightPop(key);
	}

	public List<String> getListRange(String key, long start, long end) {
		return stringRedisTemplate.opsForList().range(key, start, end);
	}

	// ========== Set ==========

	public void addToSet(String key, String... values) {
		stringRedisTemplate.opsForSet().add(key, values);
	}

	public Set<String> getSetMembers(String key) {
		return stringRedisTemplate.opsForSet().members(key);
	}

	public boolean isSetMember(String key, String value) {
		return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, value));
	}

	// ========== ZSet（有序集合） ==========

	public void addToZSet(String key, String value, double score) {
		stringRedisTemplate.opsForZSet().add(key, value, score);
	}

	public Set<String> getZSetRange(String key, long start, long end) {
		return stringRedisTemplate.opsForZSet().range(key, start, end);
	}

	public Double getZSetScore(String key, String value) {
		return stringRedisTemplate.opsForZSet().score(key, value);
	}

	// ========== 通用 ==========

	public void expire(String key, Duration timeout) {
		stringRedisTemplate.expire(key, timeout);
	}

	public boolean hasKey(String key) {
		return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
	}

	public void delete(String key) {
		stringRedisTemplate.delete(key);
	}

	public Long getExpire(String key) {
		return stringRedisTemplate.getExpire(key);
	}
}
