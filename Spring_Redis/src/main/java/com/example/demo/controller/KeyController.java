package com.example.demo.controller;

import java.time.Duration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 適用於所有資料結構的 key 操作：查詢是否存在與剩餘時間、設定過期、刪除。
 * 對應指令 TTL / EXPIRE / DEL。
 */
@RestController
@RequestMapping("/api/redis/keys/{key}")
public class KeyController {

	private final RedisService redisService;

	public KeyController(RedisService redisService) {
		this.redisService = redisService;
	}

	/** key 存在時回 200（ttlSeconds 為 null 表示不過期），不存在回 404 */
	@GetMapping
	public Responses.KeyInfo get(@PathVariable String key) {
		return new Responses.KeyInfo(key, redisService.getTtl(key));
	}

	@PutMapping("/ttl")
	public Responses.KeyInfo expire(@PathVariable String key, @Valid @RequestBody Requests.Ttl request) {
		redisService.expire(key, Duration.ofSeconds(request.seconds()));
		return new Responses.KeyInfo(key, redisService.getTtl(key));
	}

	@DeleteMapping
	public ResponseEntity<Void> delete(@PathVariable String key) {
		redisService.delete(key);
		return ResponseEntity.noContent().build();
	}
}
