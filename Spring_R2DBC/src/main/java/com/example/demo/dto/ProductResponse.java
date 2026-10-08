package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.entity.Product;

public record ProductResponse(Integer id, String description, BigDecimal price) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getDescription(), product.getPrice());
	}
}
