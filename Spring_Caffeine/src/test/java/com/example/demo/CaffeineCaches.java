package com.example.demo;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;

/**
 * 測試輔助：從 Spring 的 Cache 取出 Caffeine 原生的快取，用來檢查筆數與統計。
 * CacheManager 外層包了 TransactionAwareCacheManagerProxy，所以要先拆掉 TransactionAwareCacheDecorator。
 */
public final class CaffeineCaches {

	private CaffeineCaches() {
	}

	public static com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache(CacheManager cacheManager,
			String name) {
		Cache cache = cacheManager.getCache(name);
		if (cache instanceof TransactionAwareCacheDecorator decorator) {
			cache = decorator.getTargetCache();
		}
		return ((CaffeineCache) cache).getNativeCache();
	}
}
