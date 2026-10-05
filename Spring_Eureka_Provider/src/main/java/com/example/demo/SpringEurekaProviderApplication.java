package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 書籍服務（服務提供者）的進入點。
 * classpath 有 spring-cloud-starter-netflix-eureka-client 時，啟動後會自動以 spring.application.name
 * 向 Eureka 註冊，並定期送出心跳；不需要額外的註解（舊版的 {@code @EnableEurekaClient} 已移除）。
 */
@SpringBootApplication
public class SpringEurekaProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringEurekaProviderApplication.class, args);
	}

}
