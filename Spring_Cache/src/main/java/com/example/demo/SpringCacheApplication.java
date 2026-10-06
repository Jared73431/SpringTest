package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Spring Cache 練習：以 @Cacheable / @CachePut / @CacheEvict 宣告快取，快取存放在 Redis。
 * {@code @EnableCaching}：啟用快取註解（透過 AOP proxy 攔截方法呼叫）。
 * 修正前另外加了 @EnableJpaRepositories，Spring Boot 已自動設定，不需要。
 */
@SpringBootApplication
@EnableCaching
public class SpringCacheApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCacheApplication.class, args);
	}

}
