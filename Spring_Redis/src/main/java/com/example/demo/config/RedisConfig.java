package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.example.demo.model.User;

import tools.jackson.databind.json.JsonMapper;

/**
 * Redis 序列化設定。
 *
 * <ul>
 * <li>字串、Hash、List、Set、ZSet：使用 Spring Boot 自動建立的 StringRedisTemplate，key 與 value 都以純文字存放，
 * 用 redis-cli 或 RedisInsight 也能直接看懂</li>
 * <li>User 物件：專用的 RedisTemplate&lt;String, User&gt;，以 JSON 存放，並<b>明確指定型別</b></li>
 * </ul>
 *
 * 修正前的寫法（已移除）：
 * <pre>
 * objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, DefaultTyping.NON_FINAL, ...)
 * </pre>
 * 會把類別名稱寫進 JSON（"@class": "com.example..."），讀取時依這個欄位建立<b>任意類別</b>的物件。
 * LaissezFaire 代表不做任何限制，是 Jackson 反序列化漏洞（gadget chain）的典型成因：
 * 只要有人能寫入 Redis，就可能讓應用程式建立危險的物件。明確指定型別就不需要在 JSON 中帶類別名稱。
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

	/**
	 * 存放 User 的 RedisTemplate：value 一律反序列化成 User，不讀取 JSON 中的型別資訊。
	 * JsonMapper 使用 Spring Boot 已設定好的 Jackson 3（內建 java.time 支援，不需要另外註冊 JavaTimeModule）。
	 */
	@Bean
	RedisTemplate<String, User> userRedisTemplate(RedisConnectionFactory connectionFactory, JsonMapper jsonMapper) {
		RedisTemplate<String, User> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(RedisSerializer.string());
		template.setValueSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, User.class));
		return template;
	}
}
