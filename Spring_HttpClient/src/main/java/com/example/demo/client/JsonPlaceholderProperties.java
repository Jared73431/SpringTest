package com.example.demo.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 外部 API（JSONPlaceholder）的連線設定，對應 application.properties 的 jsonplaceholder.*。
 * 用 record + @ConfigurationProperties 集中管理，比多個 @Value 更容易閱讀，也有型別檢查（Duration 可寫成 2s、500ms）。
 */
@ConfigurationProperties("jsonplaceholder")
public record JsonPlaceholderProperties(
		@DefaultValue("https://jsonplaceholder.typicode.com") String baseUrl,
		@DefaultValue("2s") Duration connectTimeout,
		@DefaultValue("5s") Duration readTimeout) {
}
