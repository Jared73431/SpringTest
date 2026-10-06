package com.example.demo.controller;

import java.util.Map;

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
 * Hash：一個 key 底下有多個 field-value，適合存放物件的各個欄位（例如 user:profile:1 的 name、city）。
 * 對應指令 HSET / HGET / HGETALL。
 */
@RestController
@RequestMapping("/api/redis/hashes/{key}")
public class HashController {

	private final RedisService redisService;

	public HashController(RedisService redisService) {
		this.redisService = redisService;
	}

	@GetMapping
	public Map<Object, Object> getAll(@PathVariable String key) {
		return redisService.getHash(key);
	}

	@PutMapping("/fields/{field}")
	public Responses.Value setField(@PathVariable String key, @PathVariable String field,
			@Valid @RequestBody Requests.Value request) {
		redisService.setHashField(key, field, request.value());
		return new Responses.Value(request.value());
	}

	@GetMapping("/fields/{field}")
	public Responses.Value getField(@PathVariable String key, @PathVariable String field) {
		return new Responses.Value(redisService.getHashField(key, field));
	}
}
