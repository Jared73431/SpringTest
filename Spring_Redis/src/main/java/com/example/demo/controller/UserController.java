package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.Requests;
import com.example.demo.model.User;
import com.example.demo.service.RedisService;

import jakarta.validation.Valid;

/**
 * 物件：把 User 轉成 JSON 存在 String 裡（key：user:{id}）。
 * 另一種做法是存成 Hash（每個欄位一個 field），可以只讀寫單一欄位，見 HashController。
 */
@RestController
@RequestMapping("/api/redis/users/{id}")
public class UserController {

	private final RedisService redisService;

	public UserController(RedisService redisService) {
		this.redisService = redisService;
	}

	@PutMapping
	public User save(@PathVariable Long id, @Valid @RequestBody Requests.UserRequest request) {
		return redisService.saveUser(id, request.name(), request.email(), request.age());
	}

	@GetMapping
	public User get(@PathVariable Long id) {
		return redisService.getUser(id);
	}
}
