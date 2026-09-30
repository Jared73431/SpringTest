package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HelloController.class)
class HelloControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void hello_shouldReturnHelloWorld_whenGetRequest() throws Exception {
		mockMvc.perform(get("/api/hello"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/plain;charset=UTF-8"))
				.andExpect(content().string("Hello World"));
	}

	// Spring Boot 2.7 會把 /api/hello/ 視為 /api/hello（回 200）；
	// Spring Framework 6（Boot 3+）預設不再比對結尾斜線，改回 404
	@Test
	void hello_shouldReturnNotFound_whenPathHasTrailingSlash() throws Exception {
		mockMvc.perform(get("/api/hello/"))
				.andExpect(status().isNotFound());
	}

	@Test
	void hello_shouldReturnMethodNotAllowed_whenPostRequest() throws Exception {
		mockMvc.perform(post("/api/hello"))
				.andExpect(status().isMethodNotAllowed());
	}

	// 依專案 URL 規則，API 統一加上 /api 前綴，舊路徑 /hello 不再提供
	@Test
	void hello_shouldReturnNotFound_whenUsingLegacyPathWithoutApiPrefix() throws Exception {
		mockMvc.perform(get("/hello"))
				.andExpect(status().isNotFound());
	}
}
