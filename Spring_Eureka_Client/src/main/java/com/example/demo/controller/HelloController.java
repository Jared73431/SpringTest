package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.client.BookClient;

/**
 * 連線測試：GET /api/hello 轉呼叫 Provider 的 /api/hello，回傳 "Hello from &lt;Provider 的實例 id&gt;"。
 * 啟動多台 Provider 時連續呼叫，會看到實例 id 輪流出現（負載平衡）。
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
