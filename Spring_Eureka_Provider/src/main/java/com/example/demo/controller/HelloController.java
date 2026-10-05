package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 連線測試：回傳處理這次請求的實例 id，例如 "Hello from 172.18.0.5:8084"。
 * 啟動兩台 Provider 時，連續呼叫 Client 的 /api/hello 會看到 id 輪流出現，可以直接觀察到負載平衡。
 */
@RestController
public class HelloController {

	private final String instanceId;

	// 與向 Eureka 註冊時使用的 instance-id 相同（設定在 application.yaml）
	public HelloController(@Value("${eureka.instance.instance-id}") String instanceId) {
		this.instanceId = instanceId;
	}

	@GetMapping("/api/hello")
	public String hello() {
		return "Hello from " + instanceId;
	}
}
