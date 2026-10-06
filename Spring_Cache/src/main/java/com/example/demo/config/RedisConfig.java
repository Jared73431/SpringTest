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
 * Redis 快取設定。所有快取設定都集中在這裡（TTL、key 前綴、序列化）。
 *
 * 修正前同時在 application.properties 寫了 spring.cache.redis.time-to-live、key-prefix 等設定，
 * 但只要自己定義 CacheManager Bean，Spring Boot 就不會再套用那些設定（key-prefix 一直沒有生效），已移除。
 *
 * <pre>
 * 快取名稱       key 範例                         value 型別
 * users          app-cache::users::1              UserDto
 * usersByEmail   app-cache::usersByEmail::a@x.com UserDto
 * allUsers       app-cache::allUsers::all         List&lt;UserDto&gt;
 * </pre>
 *
 * 每個快取綁定明確型別（JacksonJsonRedisSerializer），Redis 中只存放一般的 JSON，不帶類別名稱（@class）。
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

	@Bean
	CacheManager cacheManager(RedisConnectionFactory factory, JsonMapper jsonMapper) {
		RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
				// 10 分鐘後過期：即使漏清快取，資料最多也只會舊 10 分鐘
				.entryTtl(Duration.ofMinutes(10))
				// key 前綴：與同一個 Redis 中其他應用程式的資料區隔
				.prefixCacheNameWith("app-cache::")
				.serializeKeysWith(SerializationPair.fromSerializer(RedisSerializer.string()))
				.disableCachingNullValues();

		SerializationPair<Object> user = typed(jsonMapper, jsonMapper.getTypeFactory().constructType(UserDto.class));
		SerializationPair<Object> userList = typed(jsonMapper,
				jsonMapper.getTypeFactory().constructCollectionType(List.class, UserDto.class));

		// immediateWrites()：寫入與清除快取都等 Redis 完成才返回。
		// Spring Data Redis 4 搭配 Lettuce 時，@CacheEvict(allEntries = true) 預設以非同步方式清除（回傳 CompletableFuture），
		// 請求結束時快取可能還沒清掉，緊接著的查詢會讀到舊資料
		RedisCacheWriter cacheWriter = RedisCacheWriter.create(factory, writer -> writer.immediateWrites());

		return RedisCacheManager.builder(cacheWriter)
				.cacheDefaults(defaults)
				.withCacheConfiguration(CacheNames.USERS, defaults.serializeValuesWith(user))
				.withCacheConfiguration(CacheNames.USERS_BY_EMAIL, defaults.serializeValuesWith(user))
				.withCacheConfiguration(CacheNames.ALL_USERS, defaults.serializeValuesWith(userList))
				// 交易提交後才真正寫入 / 清除快取：交易失敗 rollback 時，快取不會留下沒有寫進資料庫的資料
				.transactionAware()
				.build();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static SerializationPair<Object> typed(JsonMapper jsonMapper, JavaType type) {
		return (SerializationPair) SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, type));
	}
}
