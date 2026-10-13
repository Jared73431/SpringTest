package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.report.ReportService;

/**
 * 三種權限的路徑（規則在 SecurityConfig）與一個用 @PreAuthorize 保護的報表。
 */
@RestController
@RequestMapping("/api")
public class DemoController {

	private final ReportService reportService;

	public DemoController(ReportService reportService) {
		this.reportService = reportService;
	}

	// 不需要登入；有登入時仍然知道是誰（permitAll 不會跳過 Security，第 2 課）
	@GetMapping("/public/hello")
	public String hello(Principal principal) {
		return "Hello, " + (principal == null ? "guest" : principal.getName()) + "!";
	}

	public record Me(String username, List<String> authorities) {
	}

	// 需要登入
	@GetMapping("/me")
	public Me me(Authentication authentication) {
		return new Me(authentication.getName(),
				authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
	}

	// 需要 ADMIN（路徑規則）
	@GetMapping("/admin/stats")
	public String adminStats() {
		return "Users: 3";
	}

	// 只要登入就能進來，但 ReportService 的 @PreAuthorize 要求 ADMIN
	@GetMapping("/reports/sales")
	public String salesReport() {
		return reportService.salesReport();
	}
}
