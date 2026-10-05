package com.example.demo.model;

/**
 * JSONPlaceholder 的貼文資料，同時作為 API 的 Request 與 Response。
 * 修正前是 Service 裡的靜態內部類別，手寫 getter / setter / toString；改用 record 後一行就能宣告。
 * 新增時 id 為 null，由外部 API 產生。
 */
public record Post(Long id, Long userId, String title, String body) {

	/** 新增用：還沒有 id */
	public static Post of(Long userId, String title, String body) {
		return new Post(null, userId, title, body);
	}
}
