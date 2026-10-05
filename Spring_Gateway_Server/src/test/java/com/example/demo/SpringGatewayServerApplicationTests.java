package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 測試不連 Eureka 註冊中心
@SpringBootTest(properties = "eureka.client.enabled=false")
class SpringGatewayServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
