package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
	void hello_shouldReturnMessage() throws Exception {
		mockMvc.perform(get("/employee/message"))
				.andExpect(status().isOk())
				.andExpect(content().string("Hello JavaInUse Called in First Service"));
	}

	// Gateway 的 StripPrefix=1 會把 /employee/message 轉成 /message，但本服務沒有這個路徑
	@Test
	void message_shouldReturnNotFound_whenPrefixStripped() throws Exception {
		mockMvc.perform(get("/message")).andExpect(status().isNotFound());
	}
}
