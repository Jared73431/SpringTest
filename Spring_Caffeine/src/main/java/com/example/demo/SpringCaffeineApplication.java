package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Spring Cache + Caffeine 練習：與 Spring_Cache 相同的 UserService 與快取註解，快取改存放在本機記憶體（Caffeine）。
 * {@code @EnableCaching}：啟用快取註解；{@code @ConfigurationPropertiesScan}：註冊 CacheProperties（app.cache.*）。
 */
@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
public class SpringCaffeineApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCaffeineApplication.class, args);
	}

}
