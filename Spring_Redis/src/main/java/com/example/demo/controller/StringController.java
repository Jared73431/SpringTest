package com.example.demo.controller;

import java.time.Duration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.Requests;
import com.example.demo.dto.Responses;
import com.example.demo.service.RedisService;

import jakarta.validation.Valid;

/**
 * String：最基本的 key-value。對應指令 SET / GET（帶過期時間時為 SET key value EX seconds）。
 */
@RestController
@RequestMapping("/api/redis/strings/{key}")
public class StringController {

	private final RedisService redisService;

	public StringController(RedisService redisService) {
		this.redisService = redisService;
	}

	// PUT：以 key 決定資源位置，重複呼叫結果相同（覆蓋）
	@PutMapping
	public Responses.StringValue set(@PathVariable String key, @Valid @RequestBody Requests.StringValue request) {
		Duration ttl = request.ttlSeconds() == null ? null : Duration.ofSeconds(request.ttlSeconds());
		redisService.setString(key, request.value(), ttl);
		return new Responses.StringValue(key, request.value(), redisService.getTtl(key));
	}

	@GetMapping
	public Responses.StringValue get(@PathVariable String key) {
		return new Responses.StringValue(key, redisService.getString(key), redisService.getTtl(key));
	}
}
