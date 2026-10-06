package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class SpringWebfluxApplicationTests {

	@Autowired
	private ApplicationContext context;

	// 修正前同時引入 starter-web 與 starter-webflux，Spring Boot 選擇了 Spring MVC；只保留 webflux 後才是真正的 WebFlux
	@Test
	void context_shouldBeReactive_whenOnlyWebfluxStarterIsPresent() {
		assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
	}
}
