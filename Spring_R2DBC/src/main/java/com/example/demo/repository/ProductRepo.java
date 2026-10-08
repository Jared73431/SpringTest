package com.example.demo.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.demo.entity.Product;

public interface ProductRepo extends ReactiveCrudRepository<Product, Integer> {
}
