package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;

import reactor.core.publisher.Flux;

@RestController
public class ProductController {

	@Autowired
	private ProductService productService;
	
	@GetMapping("/findAll")
	public Flux<Product> findall(){
		
		return productService.findall();
	}
	
	@PostMapping("/SaveProduct")
	public void saveProduct(@RequestParam(value = "description", required = true) String description,
			@RequestParam(value = "price", required = true) Double price) {
		
		Product product = new Product();
		product.setDescription(description);
		product.setPrice(price);
		productService.save(product);
	}
	
	@GetMapping("/getbyId/{Id}")
	public Product getbyId(@PathVariable(value = "Id") int Id) {
		return productService.getone(Id);
	}
}
