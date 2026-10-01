package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 啟用 Spring Data JPA Auditing，讓 Entity 上的 @CreatedDate / @LastModifiedDate 生效
 * （Entity 也需要加上 @EntityListeners(AuditingEntityListener.class)）。
 * 獨立成設定類別而不放在 @SpringBootApplication 上，避免 @WebMvcTest 等切片測試因為沒有 JPA 而失敗。
 * proxyBeanMethods = false：類別內沒有 @Bean 方法互相呼叫，不需要 CGLIB 代理，可稍微加快啟動。
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingConfig {
}
