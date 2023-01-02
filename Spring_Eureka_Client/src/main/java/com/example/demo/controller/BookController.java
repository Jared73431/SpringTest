package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.BookFeignClient;

@RestController
public class BookController {

	@Autowired(required=true)
	private BookFeignClient bookFeignClient;
	
	@GetMapping("/Hello")
	public String hello() {
		return bookFeignClient.hello();
	}
	
	@GetMapping("/findall")
	public List<?> findall() {
		return bookFeignClient.findall();
	}
}
