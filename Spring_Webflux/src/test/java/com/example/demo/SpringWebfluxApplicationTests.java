package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SpringWebfluxApplicationTests {

	@Autowired
	private ApplicationContext context;

	// [Potential Bug] 同時引入 starter-web 與 starter-webflux 時，Spring Boot 選擇 Spring MVC（Servlet），WebFlux 不會啟用
	@Test
	void context_shouldBeServletBased_whenBothWebStartersArePresent() {
		assertThat(context).isInstanceOf(WebApplicationContext.class);
	}
}
