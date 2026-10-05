package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka 註冊中心（Service Registry）。
 * 各服務啟動時向這裡註冊自己的位址，並定期送出心跳；呼叫端向這裡查詢「某個服務名稱目前有哪些實例」。
 * {@code @EnableEurekaServer}：啟用 Eureka Server，並提供 dashboard（http://localhost:8761）與 REST API（/eureka/apps）。
 */
@SpringBootApplication
@EnableEurekaServer
public class SpringEurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringEurekaApplication.class, args);
	}

}
