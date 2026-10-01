package com.example.demo;

import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SpringJpa2ApplicationTests {

	@Test
	void contextLoads() {
	}

}
