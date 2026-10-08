package com.example.demo.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.service.ProductService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public Flux<ProductResponse> findAll() {
		return productService.findAll();
	}

	@GetMapping("/{id}")
	public Mono<ProductResponse> findById(@PathVariable Integer id) {
		return productService.findById(id);
	}

	@PostMapping
	public Mono<ResponseEntity<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
		return productService.create(request)
				.map(product -> ResponseEntity.created(URI.create("/api/products/" + product.id())).body(product));
	}

	@PutMapping("/{id}")
	public Mono<ProductResponse> update(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> delete(@PathVariable Integer id) {
		return productService.delete(id);
	}
}
