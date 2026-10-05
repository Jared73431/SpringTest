package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false")
class SpringEurekaClientApplicationTests {

	@Test
	void contextLoads() {
	}

}
