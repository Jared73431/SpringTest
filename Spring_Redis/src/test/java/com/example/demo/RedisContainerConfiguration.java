package com.example.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 測試用的 Redis：由 Testcontainers 啟動臨時容器，不會連到本機的 Redis。
 * {@code @ServiceConnection(name = "redis")}：Spring Boot 自動把容器的 host / port 設定成 spring.data.redis.*。
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisContainerConfiguration {

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
	}
}
