package com.example.demo.config;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.example.demo.dto.UserDto;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis 快取設定：每個快取指定自己的型別。
 *
 * <pre>
 * 快取名稱    key        value 型別
 * users       id         UserDto
 * allUsers    （固定）   List&lt;UserDto&gt;
 * </pre>
 *
 * 修正前使用 GenericJackson2JsonRedisSerializer：JSON 帶有類別名稱（@class），讀取時允許建立任意類別，
 * 有反序列化漏洞的風險；搭配可寫入任意 key 的 API，還能直接竄改快取內容。
 * 改為每個快取綁定明確型別，Redis 中只存放一般的 JSON。
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

	@Bean
	CacheManager cacheManager(RedisConnectionFactory factory, JsonMapper jsonMapper) {
		RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.serializeKeysWith(SerializationPair.fromSerializer(RedisSerializer.string()))
				.disableCachingNullValues();

		JavaType userList = jsonMapper.getTypeFactory().constructCollectionType(List.class, UserDto.class);

		// immediateWrites()：寫入與清除快取都等 Redis 完成才返回。
		// Spring Data Redis 4 搭配 Lettuce 時，@CacheEvict(allEntries = true) 預設以非同步方式清除（回傳 CompletableFuture），
		// 請求結束時快取可能還沒清掉，緊接著的查詢會讀到舊資料
		RedisCacheWriter cacheWriter = RedisCacheWriter.create(factory, writer -> writer.immediateWrites());

		return RedisCacheManager.builder(cacheWriter)
				.cacheDefaults(defaults)
				.withCacheConfiguration("users", defaults.serializeValuesWith(
						SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, UserDto.class))))
				.withCacheConfiguration("allUsers", defaults.serializeValuesWith(
						SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, userList))))
				.build();
	}
}
