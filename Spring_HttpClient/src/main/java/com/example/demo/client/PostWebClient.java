package com.example.demo.client;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.model.Post;

import io.netty.channel.ChannelOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * 用 WebClient（Spring 5+，Reactive）呼叫 JSONPlaceholder，功能與 {@link PostRestClient} 相同。
 * 回傳 Mono（0 或 1 筆）/ Flux（多筆）：呼叫方法時<b>還沒有送出請求</b>，要等有人訂閱（subscribe）才會執行，
 * 在 Spring MVC 的 Controller 回傳 Mono / Flux 時，由 Spring 負責訂閱並等待結果。
 *
 * 錯誤時拋出的例外（由 GlobalExceptionHandler 處理）：
 * <ul>
 * <li>外部 API 回 4xx / 5xx：WebClientResponseException</li>
 * <li>連不上、逾時：WebClientRequestException</li>
 * </ul>
 */
@Component
public class PostWebClient {

	private final WebClient webClient;

	// 注入 Spring Boot 提供的 WebClient.Builder，而不是自己呼叫 WebClient.builder()
	public PostWebClient(WebClient.Builder builder, JsonPlaceholderProperties properties) {
		// 逾時設定在底層的 Reactor Netty：連線逾時、等待回應的逾時
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis())
				.responseTimeout(properties.readTimeout());

		this.webClient = builder
				.baseUrl(properties.baseUrl())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	public Mono<Post> findById(Long id) {
		return webClient.get()
				.uri("/posts/{id}", id)
				.retrieve()
				.bodyToMono(Post.class);
	}

	public Flux<Post> findAll() {
		return webClient.get()
				.uri("/posts")
				.retrieve()
				.bodyToFlux(Post.class);
	}

	public Mono<Post> create(Post post) {
		return webClient.post()
				.uri("/posts")
				.bodyValue(post)
				.retrieve()
				.bodyToMono(Post.class);
	}

	public Mono<Post> update(Long id, Post post) {
		return webClient.put()
				.uri("/posts/{id}", id)
				.bodyValue(post)
				.retrieve()
				.bodyToMono(Post.class);
	}

	public Mono<Void> delete(Long id) {
		return webClient.delete()
				.uri("/posts/{id}", id)
				.retrieve()
				.toBodilessEntity()
				.then();
	}
}
