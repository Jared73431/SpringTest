package com.example.demo.config;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 自訂 Interceptor：每個請求都自動加上 User-Agent。
 * 修正前每個請求都手動 addHeader("User-Agent", ...)；共通的 header（User-Agent、認證 token、trace id）
 * 適合集中在 interceptor 處理，個別請求就不必重複撰寫。
 *
 * Interceptor 的模式：取得原始請求 → 修改（Request 不可變，要用 newBuilder() 產生新的）→ chain.proceed() 交給下一層。
 */
public class UserAgentInterceptor implements Interceptor {

	private final String userAgent;

	public UserAgentInterceptor(String userAgent) {
		this.userAgent = userAgent;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request().newBuilder()
				.header("User-Agent", userAgent)
				.build();
		return chain.proceed(request);
	}
}
