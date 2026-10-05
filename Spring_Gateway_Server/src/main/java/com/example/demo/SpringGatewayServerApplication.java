package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway：所有外部請求的統一入口，依路徑轉送到後端服務。
 * 路由設定在 application.yml；Spring Cloud Gateway 建立在 WebFlux 與 Netty 之上（非 Servlet）。
 * classpath 有 Eureka Client，啟動後也會以 api-gateway 的名稱註冊到 Eureka，並能用 lb:// 轉送到 Eureka 上的服務。
 */
@SpringBootApplication
public class SpringGatewayServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringGatewayServerApplication.class, args);
	}

}
