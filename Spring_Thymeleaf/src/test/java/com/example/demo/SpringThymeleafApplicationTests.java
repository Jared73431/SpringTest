package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Baseline：鎖定重構前的行為。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpringThymeleafApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Environment environment;

	@Test
	void hello_shouldRenderStaticHelloPage() throws Exception {
		mockMvc.perform(get("/hello"))
				.andExpect(status().isOk())
				.andExpect(view().name("hello"))
				.andExpect(content().string(containsString("<h1>Hello World！</h1>")));
	}

	// [Potential Bug] application.yml 的 thymeleaf: 寫在最外層，不是 spring.thymeleaf.*，設定完全沒有作用
	@Test
	void thymeleafSettings_shouldNotBeBound() {
		assertThat(environment.getProperty("spring.thymeleaf.mode")).isNull();
		assertThat(environment.getProperty("thymeleaf.mode")).isEqualTo("HTML5");
	}
}
