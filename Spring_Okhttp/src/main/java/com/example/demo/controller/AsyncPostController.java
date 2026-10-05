package com.example.demo.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.client.PostOkHttpClient;
import com.example.demo.model.Post;

/**
 * 非同步版本：GET /api/async/posts/{id}。
 *
 * 修正前的 /test-async 立即回傳「已啟動」，實際結果只印在 console，呼叫端拿不到資料也不知道成功與否。
 * 改為回傳 CompletableFuture：Spring MVC 先釋放處理請求的執行緒，等 OkHttp 回應後再寫出結果；
 * 失敗時（completeExceptionally）一樣交給 GlobalExceptionHandler。
 */
@RestController
public class AsyncPostController {

	private final PostOkHttpClient client;

	public AsyncPostController(PostOkHttpClient client) {
		this.client = client;
	}

	@GetMapping("/api/async/posts/{id}")
	public CompletableFuture<Post> findById(@PathVariable Long id) {
		return client.findByIdAsync(id);
	}
}
