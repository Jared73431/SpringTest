package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jsonplaceholder.base-url=http://localhost:1") // 不連外部網路
class SpringOkhttpApplicationTests {

	@Test
	void contextLoads() {
	}

}
