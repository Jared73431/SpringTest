package com.example.demo.controller;

import java.time.Instant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 需要 Bearer Token 的 API。登入者的資訊直接來自 Token 的內容，不需要查資料庫或 Session。
 */
@RestController
@RequestMapping("/api")
public class DemoController {

	public record Me(String username, String scope, Instant issuedAt, Instant expiresAt) {
	}

	@GetMapping("/me")
	public Me me(@AuthenticationPrincipal Jwt jwt) {
		return new Me(jwt.getSubject(), jwt.getClaimAsString("scope"), jwt.getIssuedAt(), jwt.getExpiresAt());
	}

	// 需要 SCOPE_ADMIN
	@GetMapping("/admin/stats")
	public String adminStats() {
		return "Users: 2";
	}

	// 無狀態的 API 不需要 CSRF token（第 3 課）
	@PostMapping("/messages")
	public String postMessage(@RequestBody String message, @AuthenticationPrincipal Jwt jwt) {
		return jwt.getSubject() + " says: " + message;
	}
}
