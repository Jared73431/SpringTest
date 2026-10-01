package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 連線測試用端點：Feign Client 呼叫 GET /api/hello，用來確認兩個服務之間連得通。
 */
@RestController
public class HelloController {

	@GetMapping("/api/hello")
	public String hello() {
		return "Hello";
	}
}
