package com.example.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepo;
import com.example.demo.service.ProductService;

import reactor.core.publisher.Flux;

@Service
public class ProductServiceImpl implements ProductService{

	@Autowired
	private ProductRepo productRepo;

	@Override
	public Flux<Product> findall() {
		
//		productRepo.findAll().
		return productRepo.findAll();
	}

	@Override
	public void save(Product product) {
		productRepo.save(product.setAsNew()).block();
		
	}

	@Override
	public Product getone(Integer id) {
		return productRepo.findById(id).block();
	}
	
	
}
