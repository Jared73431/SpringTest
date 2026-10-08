package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.repository.ProductRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 修正前：ProductService 介面 + ProductServiceImpl，Service 內用 block() 把 Mono 變回同步的結果，再交給 Spring MVC。
 * 修正後：全程回傳 Mono / Flux，由 WebFlux 訂閱（只有一個實作，所以不再保留介面）。
 */
@Service
public class ProductService {

	private final ProductRepo productRepo;

	public ProductService(ProductRepo productRepo) {
		this.productRepo = productRepo;
	}

	public Flux<ProductResponse> findAll() {
		return productRepo.findAll().map(ProductResponse::from);
	}

	public Mono<ProductResponse> findById(Integer id) {
		return findProduct(id).map(ProductResponse::from);
	}

	public Mono<ProductResponse> create(ProductRequest request) {
		return productRepo.save(new Product(request.description(), request.price())).map(ProductResponse::from);
	}

	public Mono<ProductResponse> update(Integer id, ProductRequest request) {
		return findProduct(id)
				.flatMap(product -> {
					product.setDescription(request.description());
					product.setPrice(request.price());
					return productRepo.save(product);
				})
				.map(ProductResponse::from);
	}

	public Mono<Void> delete(Integer id) {
		return findProduct(id).flatMap(productRepo::delete);
	}

	// 查不到時是空的 Mono；轉成錯誤訊號，Controller 才會回傳 404 而不是 200 且沒有內容
	private Mono<Product> findProduct(Integer id) {
		return productRepo.findById(id).switchIfEmpty(Mono.error(() -> new ProductNotFoundException(id)));
	}
}
