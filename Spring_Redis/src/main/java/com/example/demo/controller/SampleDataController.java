package com.example.demo.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.RedisService;

/**
 * 建立一組範例資料，方便用 RedisInsight 或 API 觀察各種資料結構。
 */
@RestController
public class SampleDataController {

	private final RedisService redisService;

	public SampleDataController(RedisService redisService) {
		this.redisService = redisService;
	}

	/** 回傳建立的 key */
	@PostMapping("/api/redis/sample-data")
	public List<String> create() {
		redisService.setString("sample:greeting", "Hello Redis!", null);
		redisService.setString("sample:otp", "123456", Duration.ofMinutes(5));
		redisService.saveUser(1L, "張三", "zhangsan@example.com", 25);
		redisService.setHashField("user:profile:1", "name", "張三");
		redisService.setHashField("user:profile:1", "city", "台北");
		redisService.pushRight("todo:list", "學習 Redis");
		redisService.pushRight("todo:list", "寫程式");
		redisService.addToSet("tags", List.of("Java", "Spring", "Redis"));
		redisService.addToZSet("scores", "Alice", 100);
		redisService.addToZSet("scores", "Bob", 85);
		return List.of("sample:greeting", "sample:otp", "user:1", "user:profile:1", "todo:list", "tags", "scores");
	}
}
