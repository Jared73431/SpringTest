package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * HTTP Client 練習：以 RestClient（同步）與 WebClient（Reactive）兩種方式呼叫外部 API（JSONPlaceholder）。
 * {@code @ConfigurationPropertiesScan}：掃描並註冊 {@code @ConfigurationProperties} 的 record（JsonPlaceholderProperties）。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringHttpClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringHttpClientApplication.class, args);
	}

}
