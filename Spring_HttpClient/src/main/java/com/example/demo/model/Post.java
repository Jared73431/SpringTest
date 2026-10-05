package com.example.demo.model;

/**
 * JSONPlaceholder 的貼文資料，同時作為 API 的 Request 與 Response。
 * 使用 record：不可變、自動產生建構子與 accessor（title()），Jackson 依欄位名稱對應 JSON。
 * 新增時 id 為 null，由外部 API 產生。
 */
public record Post(Long id, Long userId, String title, String body) {

	/** 新增用：還沒有 id */
	public static Post of(Long userId, String title, String body) {
		return new Post(null, userId, title, body);
	}
}
