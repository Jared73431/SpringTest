package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * OkHttpClient 註冊成 Spring Bean，整個應用程式共用同一個實例。
 * OkHttpClient 內含連線池與執行緒池，官方建議共用一個，不要每次請求都 new 一個。
 */
@Configuration(proxyBeanMethods = false)
public class OkHttpConfig {

	private static final Logger log = LoggerFactory.getLogger("okhttp");

	@Bean
	OkHttpClient okHttpClient(JsonPlaceholderProperties properties) {
		// 日誌改走 SLF4J（預設直接印到 stdout）；BASIC 只記錄方法、URL、狀態碼、耗時
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::info);
		logging.setLevel(properties.logLevel());
		// 等級調高到 HEADERS / BODY 時，避免 token 被寫進 log
		logging.redactHeader("Authorization");

		return new OkHttpClient.Builder()
				// Interceptor 依加入順序執行：先加上 User-Agent，logging 才記錄得到最終送出的 header
				.addInterceptor(new UserAgentInterceptor("Spring-Okhttp-Demo/1.0"))
				.addInterceptor(logging)
				.connectTimeout(properties.connectTimeout())
				.readTimeout(properties.readTimeout())
				.writeTimeout(properties.writeTimeout())
				.build();
	}
}
