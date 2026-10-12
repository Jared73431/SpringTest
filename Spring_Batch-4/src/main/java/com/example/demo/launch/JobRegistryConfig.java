package com.example.demo.launch;

import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 依名稱找 Job 的 JobRegistry。
 *
 * <p>
 * Spring Boot 4 不會自動建立 JobRegistry Bean（實測：注入時 NoSuchBeanDefinitionException）。
 * Spring Batch 6 的 MapJobRegistry 宣告成 Bean 之後，會在所有 singleton 建立完成時自動註冊容器中的每一個 Job，
 * 不需要像 Batch 4、5 時另外設定 JobRegistryBeanPostProcessor。
 */
@Configuration
public class JobRegistryConfig {

	@Bean
	public MapJobRegistry jobRegistry() {
		return new MapJobRegistry();
	}
}
