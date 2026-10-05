package com.example.demo.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.demo.client.PostOkHttpClient;
import com.example.demo.model.Post;

/**
 * 啟動後自動示範同步 GET / POST 與非同步 GET，結果輸出到 log。
 * 修正前寫在啟動類別裡（SpringOkhttpApplication implements CommandLineRunner），測試啟動時也會連外部網路。
 *
 * <ul>
 * <li>demo.runner.enabled=false 可以關閉（測試時關閉）</li>
 * <li>外部 API 無法使用時只記錄警告，不影響應用程式啟動</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "demo.runner.enabled", havingValue = "true", matchIfMissing = true)
public class DemoRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);

	private final PostOkHttpClient client;

	public DemoRunner(PostOkHttpClient client) {
		this.client = client;
	}

	@Override
	public void run(String... args) {
		try {
			log.info("=== OkHttp 示範 ===");
			log.info("同步 GET  /posts/1 -> {}", client.findById(1L));
			log.info("同步 POST /posts   -> {}", client.create(Post.of(1L, "Spring Boot + OkHttp 測試", "這是用 OkHttp 送出的 POST 請求")));
			// CommandLineRunner 需要等結果才能輸出，因此 join()；Controller 中則直接回傳 CompletableFuture
			log.info("非同步 GET /posts/2 -> {}", client.findByIdAsync(2L).join());
		} catch (RuntimeException ex) {
			log.warn("OkHttp 示範失敗（外部 API 無法使用，應用程式照常啟動）：{}", ex.getMessage());
		}
	}
}
