package com.example.demo.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 各快取的容量上限與存活時間，對應 application.properties 的 app.cache.*。
 *
 * 本機快取一定要設上限：資料存在 JVM 記憶體中，沒有上限就只會一直累積，最後 OutOfMemoryError。
 * （Spring 預設的 Simple 快取就是沒有上限的 ConcurrentHashMap，不適合正式環境）
 */
@ConfigurationProperties("app.cache")
public record CacheProperties(
		@DefaultValue Spec users,
		@DefaultValue Spec usersByEmail,
		@DefaultValue("1") long allUsersMaximumSize,
		@DefaultValue("1m") Duration allUsersExpireAfterWrite) {

	/**
	 * @param maximumSize      最多存幾筆，超過時由 Caffeine 依使用頻率淘汰
	 * @param expireAfterWrite 寫入後多久過期
	 */
	public record Spec(@DefaultValue("1000") long maximumSize, @DefaultValue("10m") Duration expireAfterWrite) {
	}
}
