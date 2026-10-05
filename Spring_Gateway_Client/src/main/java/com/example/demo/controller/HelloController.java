package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 被 Gateway 轉送的後端 API。
 * 本服務只提供 /api/... 路徑，不需要知道自己在 Gateway 後面：
 * Gateway 收到 /first-service/api/hello，去掉前綴 /first-service 後轉送到這裡的 /api/hello。
 */
@RestController
public class HelloController {

	private static final Logger log = LoggerFactory.getLogger(HelloController.class);

	private final String serviceName;

	public HelloController(@Value("${spring.application.name}") String serviceName) {
		this.serviceName = serviceName;
	}

	@GetMapping("/api/hello")
	public String hello() {
		log.info("GET /api/hello called");
		return "Hello from " + serviceName;
	}
}
