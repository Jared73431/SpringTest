package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.Responses.ScoredMember;
import com.example.demo.exception.KeyNotFoundException;
import com.example.demo.model.User;

/**
 * Redis 五種資料結構的基本操作。
 * 純文字資料使用 StringRedisTemplate；User 物件使用專用的 RedisTemplate&lt;String, User&gt;（見 RedisConfig）。
 * 查不到資料時拋出 KeyNotFoundException（404）。
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

	/** timeout 為 null 表示不過期；Spring Data Redis 4 改用 Duration（set(..., long, TimeUnit) 已 deprecated） */
	public void setString(String key, String value, Duration timeout) {
		if (timeout == null) {
			stringRedisTemplate.opsForValue().set(key, value);
		} else {
			stringRedisTemplate.opsForValue().set(key, value, timeout);
		}
	}

	public String getString(String key) {
		String value = stringRedisTemplate.opsForValue().get(key);
		if (value == null) {
			throw new KeyNotFoundException("Key '" + key + "'");
		}
		return value;
	}

	// ========== User 物件（JSON） ==========

	/** 新增或覆蓋使用者；保留原本的建立時間 */
	public User saveUser(Long id, String name, String email, Integer age) {
		String key = userKey(id);
		User existing = userRedisTemplate.opsForValue().get(key);
		LocalDateTime now = LocalDateTime.now();
		User user = new User(id, name, email, age, existing != null ? existing.createTime() : now, now);
		userRedisTemplate.opsForValue().set(key, user);
		return user;
	}

	public User getUser(Long id) {
		User user = userRedisTemplate.opsForValue().get(userKey(id));
		if (user == null) {
			throw new KeyNotFoundException("User " + id);
		}
		return user;
	}

	// key 命名慣例：「類型:id」，用冒號分層，RedisInsight 等工具會以資料夾方式顯示
	private static String userKey(Long id) {
		return "user:" + id;
	}

	// ========== Hash ==========

	public void setHashField(String key, String field, String value) {
		stringRedisTemplate.opsForHash().put(key, field, value);
	}

	public String getHashField(String key, String field) {
		Object value = stringRedisTemplate.opsForHash().get(key, field);
		if (value == null) {
			throw new KeyNotFoundException("Field '" + field + "' of hash '" + key + "'");
		}
		return value.toString();
	}

	public Map<Object, Object> getHash(String key) {
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
		if (entries.isEmpty()) {
			throw new KeyNotFoundException("Hash '" + key + "'");
		}
		return entries;
	}

	// ========== List ==========

	public void pushLeft(String key, String value) {
		stringRedisTemplate.opsForList().leftPush(key, value);
	}

	public void pushRight(String key, String value) {
		stringRedisTemplate.opsForList().rightPush(key, value);
	}

	public String popLeft(String key) {
		return requireElement(key, stringRedisTemplate.opsForList().leftPop(key));
	}

	public String popRight(String key) {
		return requireElement(key, stringRedisTemplate.opsForList().rightPop(key));
	}

	public List<String> getList(String key) {
		return stringRedisTemplate.opsForList().range(key, 0, -1);
	}

	private static String requireElement(String key, String value) {
		if (value == null) {
			throw new KeyNotFoundException("Element in list '" + key + "'");
		}
		return value;
	}

	// ========== Set ==========

	public void addToSet(String key, List<String> members) {
		stringRedisTemplate.opsForSet().add(key, members.toArray(String[]::new));
	}

	public Set<String> getSet(String key) {
		return stringRedisTemplate.opsForSet().members(key);
	}

	public boolean isMember(String key, String member) {
		return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, member));
	}

	// ========== ZSet（有序集合） ==========

	public void addToZSet(String key, String member, double score) {
		stringRedisTemplate.opsForZSet().add(key, member, score);
	}

	/** 依分數由小到大，連同分數一起回傳 */
	public List<ScoredMember> getZSet(String key) {
		return stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, -1).stream()
				.map(tuple -> new ScoredMember(tuple.getValue(), tuple.getScore()))
				.toList();
	}

	public double getScore(String key, String member) {
		Double score = stringRedisTemplate.opsForZSet().score(key, member);
		if (score == null) {
			throw new KeyNotFoundException("Member '" + member + "' of sorted set '" + key + "'");
		}
		return score;
	}

	// ========== Key ==========

	/**
	 * 剩餘存活秒數；null 表示不會過期。
	 * Redis 的 TTL 指令以特殊值表示狀態：-2 = key 不存在、-1 = 沒有設定過期時間。
	 */
	public Long getTtl(String key) {
		Long ttl = stringRedisTemplate.getExpire(key);
		if (ttl == null || ttl == -2) {
			throw new KeyNotFoundException("Key '" + key + "'");
		}
		return ttl == -1 ? null : ttl;
	}

	public void expire(String key, Duration timeout) {
		if (!Boolean.TRUE.equals(stringRedisTemplate.expire(key, timeout))) {
			throw new KeyNotFoundException("Key '" + key + "'");
		}
	}

	public void delete(String key) {
		if (!Boolean.TRUE.equals(stringRedisTemplate.delete(key))) {
			throw new KeyNotFoundException("Key '" + key + "'");
		}
	}
}
