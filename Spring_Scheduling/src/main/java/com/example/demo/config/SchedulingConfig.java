package com.example.demo.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * 排程的設定。
 *
 * <p>
 * \@EnableScheduling 放在獨立的設定類別（修正前放在主程式上）：需要時可以用 property 或 profile 整個關閉，
 * 而且 \@WebMvcTest 之類的切片測試不會載入它。
 *
 * <p>
 * ShedLock：多台主機同時執行同一個排程時，只有搶到資料庫中那一列鎖的主機會執行（第 6 課）。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulingConfig {

	@Bean
	LockProvider lockProvider(DataSource dataSource) {
		return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
				.withJdbcTemplate(new JdbcTemplate(dataSource))
				// 用資料庫的時間判斷鎖是否到期，不受各台主機時鐘誤差的影響
				.usingDbTime()
				.build());
	}
}
