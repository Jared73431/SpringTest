package com.example.demo.service;

import com.example.demo.entity.Product;

import reactor.core.publisher.Flux;

public interface ProductService {

	public Flux<Product> findall();
	
	public void save(Product product);
	
	public Product getone(Integer id);
}
