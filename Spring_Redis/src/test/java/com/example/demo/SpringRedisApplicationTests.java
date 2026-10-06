package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(RedisContainerConfiguration.class)
class SpringRedisApplicationTests {

	@Test
	void contextLoads() {
	}

}
