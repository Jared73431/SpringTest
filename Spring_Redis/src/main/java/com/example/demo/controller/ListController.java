package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.Requests;
import com.example.demo.dto.Responses;
import com.example.demo.service.RedisService;

import jakarta.validation.Valid;

/**
 * List：有順序、可重複，兩端都能加入與取出。
 * 從同一端進出 = 堆疊（stack）；一端進、另一端出 = 佇列（queue）。對應指令 LPUSH / RPUSH / LPOP / RPOP / LRANGE。
 */
@RestController
@RequestMapping("/api/redis/lists/{key}")
public class ListController {

	private final RedisService redisService;

	public ListController(RedisService redisService) {
		this.redisService = redisService;
	}

	@GetMapping
	public List<String> getAll(@PathVariable String key) {
		return redisService.getList(key);
	}

	@PostMapping("/left")
	public List<String> pushLeft(@PathVariable String key, @Valid @RequestBody Requests.Value request) {
		redisService.pushLeft(key, request.value());
		return redisService.getList(key);
	}

	@PostMapping("/right")
	public List<String> pushRight(@PathVariable String key, @Valid @RequestBody Requests.Value request) {
		redisService.pushRight(key, request.value());
		return redisService.getList(key);
	}

	// DELETE：取出並移除一個元素；list 已空時回 404
	@DeleteMapping("/left")
	public Responses.Value popLeft(@PathVariable String key) {
		return new Responses.Value(redisService.popLeft(key));
	}

	@DeleteMapping("/right")
	public Responses.Value popRight(@PathVariable String key) {
		return new Responses.Value(redisService.popRight(key));
	}
}
