package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 書籍 Client 服務的進入點，透過 Feign 呼叫 Spring_Feign_Server。
 * {@code @EnableFeignClients}：掃描本 package 及子 package 中標註 {@code @FeignClient} 的介面，產生實作並註冊成 Bean。
 */
@EnableFeignClients
@SpringBootApplication
public class SpringFeignClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringFeignClientApplication.class, args);
	}

}
