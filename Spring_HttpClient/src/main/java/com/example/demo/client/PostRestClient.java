package com.example.demo.client;

import java.net.http.HttpClient;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.demo.model.Post;

/**
 * 用 RestClient（Spring 6.1+，同步）呼叫 JSONPlaceholder。
 * 呼叫後直接拿到結果（Post、List&lt;Post&gt;），寫法和一般方法呼叫相同，適合 Spring MVC 應用程式。
 *
 * 錯誤時拋出的例外（由 GlobalExceptionHandler 處理）：
 * <ul>
 * <li>外部 API 回 4xx / 5xx：HttpClientErrorException / HttpServerErrorException（RestClientResponseException 的子類別）</li>
 * <li>連不上、逾時：ResourceAccessException</li>
 * </ul>
 */
@Component
public class PostRestClient {

	private final RestClient restClient;

	// 注入 Spring Boot 提供的 RestClient.Builder（已套用 Jackson、observability 等設定），而不是自己呼叫 RestClient.builder()
	public PostRestClient(RestClient.Builder builder, JsonPlaceholderProperties properties) {
		// 使用 JDK 內建的 HttpClient（Java 11+）作為底層連線
		// version(HTTP_1_1)：JDK HttpClient 預設 HTTP/2，在 http://（非 HTTPS）連線上會先送出 h2c 升級請求；
		// 部分伺服器（例如 WireMock 使用的 Jetty）收到「帶 body 的升級請求」（PUT）會直接斷線（EOFException），因此固定使用 HTTP/1.1
		HttpClient httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());

		this.restClient = builder
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	public Post findById(Long id) {
		return restClient.get()
				.uri("/posts/{id}", id)
				.retrieve()
				.body(Post.class);
	}

	// 回傳泛型集合時，用 ParameterizedTypeReference 保留 List<Post> 的型別資訊
	public List<Post> findAll() {
		return restClient.get()
				.uri("/posts")
				.retrieve()
				.body(new ParameterizedTypeReference<List<Post>>() {
				});
	}

	public Post create(Post post) {
		return restClient.post()
				.uri("/posts")
				.body(post)
				.retrieve()
				.body(Post.class);
	}

	public Post update(Long id, Post post) {
		return restClient.put()
				.uri("/posts/{id}", id)
				.body(post)
				.retrieve()
				.body(Post.class);
	}

	public void delete(Long id) {
		restClient.delete()
				.uri("/posts/{id}", id)
				.retrieve()
				.toBodilessEntity();
	}
}
