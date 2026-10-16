package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.Requests;
import com.example.demo.model.Product;
import com.example.demo.service.RedisService;

import jakarta.validation.Valid;

/**
 * 物件：和 UserController 一樣把物件轉成 JSON 存在 String 裡（key：product:{id}），
 * 差別在於 Product 透過共用的 RedisJsonStore 存取，不需要專用的 RedisTemplate bean。
 */
@RestController
@RequestMapping("/api/redis/products/{id}")
public class ProductController {

	private final RedisService redisService;

	public ProductController(RedisService redisService) {
		this.redisService = redisService;
	}

	@PutMapping
	public Product save(@PathVariable Long id, @Valid @RequestBody Requests.ProductRequest request) {
		return redisService.saveProduct(id, request.name(), request.price());
	}

	@GetMapping
	public Product get(@PathVariable Long id) {
		return redisService.getProduct(id);
	}
}
