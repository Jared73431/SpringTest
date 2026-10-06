package com.example.demo.controller;

import java.util.Set;

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
 * Set：不重複、沒有順序。適合標籤、去重、判斷「是否已經做過」（例如已按讚的使用者）。
 * 對應指令 SADD / SMEMBERS / SISMEMBER。
 */
@RestController
@RequestMapping("/api/redis/sets/{key}")
public class SetController {

	private final RedisService redisService;

	public SetController(RedisService redisService) {
		this.redisService = redisService;
	}

	@GetMapping
	public Set<String> getAll(@PathVariable String key) {
		return redisService.getSet(key);
	}

	@PostMapping
	public Set<String> add(@PathVariable String key, @Valid @RequestBody Requests.SetMembers request) {
		redisService.addToSet(key, request.members());
		return redisService.getSet(key);
	}

	@GetMapping("/members/{member}")
	public Responses.Membership isMember(@PathVariable String key, @PathVariable String member) {
		return new Responses.Membership(member, redisService.isMember(key, member));
	}
}
