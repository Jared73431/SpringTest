package com.example.demo.config;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * 啟用 R2DBC Auditing：儲存前自動填入 @CreatedDate / @LastModifiedDate（和 JPA 的 @EnableJpaAuditing 相同的概念）。
 *
 * <p>
 * 時間截到微秒：PostgreSQL 的 TIMESTAMP 只存到微秒，Java 的時間可以到奈秒。不截斷的話，新增時回應中的時間
 * （記憶體中的值，例如 .449447900）會和之後查詢到的時間（資料庫四捨五入後的 .449448）不一樣。
 */
@Configuration(proxyBeanMethods = false)
@EnableR2dbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class R2dbcConfig {

	@Bean
	DateTimeProvider auditingDateTimeProvider() {
		return () -> Optional.of(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
	}
}
