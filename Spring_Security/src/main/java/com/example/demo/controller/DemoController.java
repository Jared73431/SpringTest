package com.example.demo.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 沒有任何 Security 設定：只要加上 spring-boot-starter-security，這裡的每一個路徑都需要登入。
 */
@RestController
@RequestMapping("/api")
public class DemoController {

	// Principal：Spring Security 放入的「目前登入的使用者」
	@GetMapping("/hello")
	public String hello(Principal principal) {
		return "Hello, " + principal.getName() + "!";
	}

	// 會改變資料的請求（POST、PUT、DELETE）預設需要 CSRF token（第 2 課）
	@PostMapping("/messages")
	public String postMessage(@RequestBody String message, Principal principal) {
		return principal.getName() + " says: " + message;
	}
}
