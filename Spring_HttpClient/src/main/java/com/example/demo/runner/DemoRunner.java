package com.example.demo.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientException;

import com.example.demo.client.PostRestClient;
import com.example.demo.client.PostWebClient;
import com.example.demo.model.Post;

/**
 * 啟動後自動示範 GET / POST / PUT / DELETE，結果輸出到 log。
 *
 * <ul>
 * <li>demo.runner.enabled=false 可以關閉（測試時關閉，避免啟動時就呼叫外部 API）</li>
 * <li>外部 API 無法使用時只記錄警告，<b>不會讓應用程式啟動失敗</b>
 * （修正前用 block() 直接拋出例外，沒有網路就無法啟動）</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "demo.runner.enabled", havingValue = "true", matchIfMissing = true)
public class DemoRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);

	private final PostRestClient postRestClient;

	private final PostWebClient postWebClient;

	public DemoRunner(PostRestClient postRestClient, PostWebClient postWebClient) {
		this.postRestClient = postRestClient;
		this.postWebClient = postWebClient;
	}

	@Override
	public void run(String... args) {
		try {
			log.info("=== HTTP Client 示範（RestClient，同步） ===");
			log.info("GET    /posts/1 -> {}", postRestClient.findById(1L));
			log.info("POST   /posts   -> {}", postRestClient.create(Post.of(1L, "我的新貼文", "這是一個測試貼文內容")));
			log.info("PUT    /posts/1 -> {}", postRestClient.update(1L, new Post(1L, 1L, "更新後的標題", "更新後的內容")));
			postRestClient.delete(1L);
			log.info("DELETE /posts/1 -> 完成");

			log.info("=== HTTP Client 示範（WebClient，Reactive） ===");
			// CommandLineRunner 不是 Reactive 環境，這裡用 block() 等待結果；在 Controller 中則直接回傳 Flux，不需要 block()
			postWebClient.findAll()
					.take(3)
					.doOnNext(post -> log.info("GET    /posts   -> #{} {}", post.id(), post.title()))
					.blockLast();
		} catch (RestClientException | WebClientException ex) {
			log.warn("HTTP Client 示範失敗（外部 API 無法使用，應用程式照常啟動）：{}", ex.getMessage());
		}
	}
}
