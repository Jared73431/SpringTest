package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.client.BookClient;

/**
 * 連線測試：GET /api/hello 轉呼叫 Server 的 /api/hello，回傳 "Hello" 表示兩個服務之間連得通。
 */
@RestController
public class HelloController {

	private final BookClient bookClient;

	public HelloController(BookClient bookClient) {
		this.bookClient = bookClient;
	}

	@GetMapping("/api/hello")
	public String hello() {
		return bookClient.hello();
	}
}
