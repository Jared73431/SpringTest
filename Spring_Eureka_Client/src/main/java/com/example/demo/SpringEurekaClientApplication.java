package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 書籍 Client 服務（服務消費者）的進入點，透過 Eureka 找到 Spring_Eureka_Provider，再用 Feign 呼叫。
 * {@code @EnableFeignClients}：掃描標註 {@code @FeignClient} 的介面，產生實作並註冊成 Bean。
 * 本身也會以 spring.application.name（service-consumer）向 Eureka 註冊。
 */
@SpringBootApplication
@EnableFeignClients
public class SpringEurekaClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringEurekaClientApplication.class, args);
	}

}
