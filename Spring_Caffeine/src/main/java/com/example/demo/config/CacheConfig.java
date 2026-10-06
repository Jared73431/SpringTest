package com.example.demo.config;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caffeine 快取設定。與 Spring_Cache 的 RedisConfig 對照：UserService 的快取註解完全相同，只有這裡不同。
 *
 * <pre>
 * 快取           上限      存活時間   內容
 * users          1000 筆   10 分鐘    UserDto（依 id）
 * usersByEmail   1000 筆   10 分鐘    UserDto（依 email）
 * allUsers       1 筆      1 分鐘     List&lt;UserDto&gt;
 * </pre>
 *
 * 與 Redis 的差異：
 * <ul>
 * <li>存放在 JVM 記憶體，直接保存 Java 物件，<b>不需要序列化</b>，也沒有網路延遲</li>
 * <li>每台應用程式各自一份：A 台修改資料只會清除 A 台的快取，B 台在過期前仍可能回傳舊資料</li>
 * <li>必須設定容量上限；超過時 Caffeine 以 W-TinyLFU 演算法淘汰「最不可能再被使用」的資料</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class CacheConfig {

	@Bean
	CacheManager cacheManager(CacheProperties properties) {
		CaffeineCacheManager caffeine = new CaffeineCacheManager();
		// 不快取 null（與 Redis 版本的 disableCachingNullValues() 相同）；查無資料由 unless = "#result == null" 處理
		caffeine.setAllowNullValues(false);

		caffeine.registerCustomCache(CacheNames.USERS, build(properties.users().maximumSize(),
				properties.users().expireAfterWrite()));
		caffeine.registerCustomCache(CacheNames.USERS_BY_EMAIL, build(properties.usersByEmail().maximumSize(),
				properties.usersByEmail().expireAfterWrite()));
		caffeine.registerCustomCache(CacheNames.ALL_USERS, build(properties.allUsersMaximumSize(),
				properties.allUsersExpireAfterWrite()));
		// 只允許上面三個快取：註解中的快取名稱打錯字時立即出錯，而不是默默建立一個沒有上限的新快取
		caffeine.setCacheNames(List.of());

		// CaffeineCacheManager 沒有 transactionAware() 設定，改用 Spring 提供的 proxy 包裝：
		// 交易提交後才真正寫入 / 清除快取，rollback 時快取不會留下沒有寫進資料庫的資料
		return new TransactionAwareCacheManagerProxy(caffeine);
	}

	private static Cache<Object, Object> build(long maximumSize, Duration expireAfterWrite) {
		return Caffeine.newBuilder()
				.maximumSize(maximumSize)
				.expireAfterWrite(expireAfterWrite)
				// 記錄命中、未命中、淘汰次數：Actuator 的 /actuator/metrics/cache.gets 等指標需要它
				.recordStats()
				.build();
	}
}
