package com.example.demo.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // Boot 4（Jackson 3）：GenericJackson2JsonRedisSerializer 需要 Jackson 2，已無法使用。
    // 新的 GenericJacksonJsonRedisSerializer 預設不帶型別資訊；要維持舊行為（JSON 帶 @class、允許任意類別）
    // 必須明確呼叫 enableUnsafeDefaultTyping()，名稱本身就標示了風險，重構時改為明確型別
    private static GenericJacksonJsonRedisSerializer unsafeJsonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder().enableUnsafeDefaultTyping().build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 GenericJackson2JsonRedisSerializer 來序列化和反序列化 redis 的 value 值
        // 這是推薦的現代做法，避免了 deprecated 的 setObjectMapper 方法
        GenericJacksonJsonRedisSerializer jsonSerializer = unsafeJsonSerializer();

        // 設置 value 的序列化規則和 key 的序列化規則
        template.setValueSerializer(jsonSerializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))  // 設置快取過期時間
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(unsafeJsonSerializer()))
                .disableCachingNullValues();  // 不快取 null 值

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
