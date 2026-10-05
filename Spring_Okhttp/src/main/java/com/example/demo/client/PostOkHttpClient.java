package com.example.demo.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import com.example.demo.config.JsonPlaceholderProperties;
import com.example.demo.model.Post;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 用 OkHttp 呼叫 JSONPlaceholder。
 *
 * 與 RestClient / WebClient（見 Spring_HttpClient）的差異：OkHttp 是較底層的 HTTP client，
 * 要自己組 Request、把物件轉成 JSON、檢查狀態碼、讀取並關閉 Response。
 * JSON 轉換使用 Spring Boot 已設定好的 Jackson JsonMapper（修正前另外引入 Gson，同一個專案有兩套 JSON 函式庫）。
 */
@Component
public class PostOkHttpClient {

	private static final MediaType JSON = MediaType.get("application/json");

	private final OkHttpClient okHttpClient;

	private final JsonMapper jsonMapper;

	private final String baseUrl;

	public PostOkHttpClient(OkHttpClient okHttpClient, JsonMapper jsonMapper, JsonPlaceholderProperties properties) {
		this.okHttpClient = okHttpClient;
		this.jsonMapper = jsonMapper;
		this.baseUrl = properties.baseUrl();
	}

	// ===== 同步：execute() 會阻塞目前執行緒，直到收到回應 =====

	public Post findById(Long id) {
		return execute(get("/posts/" + id), Post.class);
	}

	public List<Post> findAll() {
		String body = execute(get("/posts"));
		return jsonMapper.readValue(body, new TypeReference<List<Post>>() {
		});
	}

	public Post create(Post post) {
		Request request = new Request.Builder().url(baseUrl + "/posts").post(toJson(post)).build();
		return execute(request, Post.class);
	}

	public Post update(Long id, Post post) {
		Request request = new Request.Builder().url(baseUrl + "/posts/" + id).put(toJson(post)).build();
		return execute(request, Post.class);
	}

	public void delete(Long id) {
		execute(new Request.Builder().url(baseUrl + "/posts/" + id).delete().build());
	}

	// ===== 非同步：enqueue() 立即返回，回應由 OkHttp 的執行緒呼叫 Callback =====

	/**
	 * 把 OkHttp 的 Callback 轉成 CompletableFuture：成功時 complete，失敗時 completeExceptionally。
	 * Spring MVC 的 Controller 可以直接回傳 CompletableFuture，等它完成後再寫出回應。
	 */
	public CompletableFuture<Post> findByIdAsync(Long id) {
		CompletableFuture<Post> future = new CompletableFuture<>();
		okHttpClient.newCall(get("/posts/" + id)).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				future.completeExceptionally(new UpstreamUnavailableException(e));
			}

			@Override
			public void onResponse(Call call, Response response) {
				// try-with-resources：無論成功或失敗都會關閉 Response（修正前讀取失敗時不會關閉）
				try (response) {
					future.complete(jsonMapper.readValue(readBody(response), Post.class));
				} catch (IOException e) {
					future.completeExceptionally(new UpstreamUnavailableException(e));
				} catch (RuntimeException e) {
					future.completeExceptionally(e);
				}
			}
		});
		return future;
	}

	// ===== 共用的小工具 =====

	private Request get(String path) {
		return new Request.Builder().url(baseUrl + path).get().build();
	}

	// RequestBody 已帶有 Content-Type，不需要再 addHeader("Content-Type", ...)
	private RequestBody toJson(Post post) {
		return RequestBody.create(jsonMapper.writeValueAsString(post), JSON);
	}

	private <T> T execute(Request request, Class<T> type) {
		return jsonMapper.readValue(execute(request), type);
	}

	// Response 必須關閉，否則連線不會回到連線池；try-with-resources 是最安全的寫法
	private String execute(Request request) {
		try (Response response = okHttpClient.newCall(request).execute()) {
			return readBody(response);
		} catch (IOException e) {
			throw new UpstreamUnavailableException(e);
		}
	}

	// OkHttp 對 4xx / 5xx 不會拋出例外，要自己檢查
	private static String readBody(Response response) throws IOException {
		if (!response.isSuccessful()) {
			throw new UpstreamResponseException(response.code());
		}
		return response.body().string();
	}
}
