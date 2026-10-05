package com.example.demo.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * 外部 API（JSONPlaceholder）與 OkHttpClient 的設定，對應 application.properties 的 jsonplaceholder.*。
 *
 * @param logLevel HttpLoggingInterceptor 的記錄等級：NONE / BASIC / HEADERS / BODY
 */
@ConfigurationProperties("jsonplaceholder")
public record JsonPlaceholderProperties(
		@DefaultValue("https://jsonplaceholder.typicode.com") String baseUrl,
		@DefaultValue("2s") Duration connectTimeout,
		@DefaultValue("5s") Duration readTimeout,
		@DefaultValue("5s") Duration writeTimeout,
		@DefaultValue("BASIC") HttpLoggingInterceptor.Level logLevel) {
}
