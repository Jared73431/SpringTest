package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * OkHttp 練習：以 OkHttp 呼叫外部 API（JSONPlaceholder），提供同步與非同步兩種 API。
 * {@code @ConfigurationPropertiesScan}：註冊 JsonPlaceholderProperties（record + @ConfigurationProperties）。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringOkhttpApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringOkhttpApplication.class, args);
	}

}
