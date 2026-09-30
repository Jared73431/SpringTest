package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HelloController.class)
class HelloControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void hello_shouldReturnHelloWorld_whenGetRequest() throws Exception {
		mockMvc.perform(get("/hello"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/plain;charset=UTF-8"))
				.andExpect(content().string("Hello World"));
	}

	@Test
	void hello_shouldReturnOk_whenPathHasTrailingSlash() throws Exception {
		mockMvc.perform(get("/hello/"))
				.andExpect(status().isOk());
	}

	@Test
	void hello_shouldReturnMethodNotAllowed_whenPostRequest() throws Exception {
		mockMvc.perform(post("/hello"))
				.andExpect(status().isMethodNotAllowed());
	}
}
