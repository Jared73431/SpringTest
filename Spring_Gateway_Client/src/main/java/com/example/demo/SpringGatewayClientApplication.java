package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * first-service：放在 Spring_Gateway_Server 後面的後端服務（一般的 Spring MVC 應用程式）。
 * 名稱雖然叫 Gateway Client，實際上是「被 Gateway 轉送的一方」，本身不需要任何 Gateway 相關依賴。
 */
@SpringBootApplication
public class SpringGatewayClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringGatewayClientApplication.class, args);
	}

}
