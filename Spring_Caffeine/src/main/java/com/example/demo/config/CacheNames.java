package com.example.demo.config;

/**
 * 快取名稱集中定義，避免在註解與設定中重複寫字串（打錯字不會有編譯錯誤，只會變成另一個快取）。
 */
public final class CacheNames {

	public static final String USERS = "users";

	public static final String USERS_BY_EMAIL = "usersByEmail";

	public static final String ALL_USERS = "allUsers";

	private CacheNames() {
	}
}
