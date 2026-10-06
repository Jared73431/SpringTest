package com.example.demo.controller;

import java.util.List;

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
 * ZSet（Sorted Set）：不重複，每個成員帶一個分數，依分數排序。
 * 對應指令 ZADD / ZRANGE ... WITHSCORES / ZSCORE；排行榜的實際用法見 LeaderboardController。
 */
@RestController
@RequestMapping("/api/redis/zsets/{key}")
public class ZSetController {

	private final RedisService redisService;

	public ZSetController(RedisService redisService) {
		this.redisService = redisService;
	}

	/** 依分數由小到大 */
	@GetMapping
	public List<Responses.ScoredMember> getAll(@PathVariable String key) {
		return redisService.getZSet(key);
	}

	@PostMapping
	public List<Responses.ScoredMember> add(@PathVariable String key,
			@Valid @RequestBody Requests.ScoredMember request) {
		redisService.addToZSet(key, request.member(), request.score());
		return redisService.getZSet(key);
	}

	@GetMapping("/members/{member}")
	public Responses.ScoredMember getScore(@PathVariable String key, @PathVariable String member) {
		return new Responses.ScoredMember(member, redisService.getScore(key, member));
	}
}
