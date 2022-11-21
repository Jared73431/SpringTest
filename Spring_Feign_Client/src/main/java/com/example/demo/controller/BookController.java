package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.controller.service.Bookservice;

@RestController
public class BookController {

	@Autowired
	private Bookservice bookservice;
	
	@GetMapping("/HelloWorld")
	public String hello() {
		return bookservice.hello();
	}
	
	@GetMapping("/findAllBook")
	public List<?> findall(){
		return bookservice.findall();
	}
	
	@PostMapping("/saveBook")
	public void saveBook(@RequestParam(value = "ISBN", required = true) int ISBN,
			@RequestParam(value = "title", required = true) String title,
			@RequestParam(value = "author", required = true) String author,
			@RequestParam(value = "year", required = true) Integer year,
			@RequestParam(value = "publisher", required = true) String publisher,
			@RequestParam(value = "cost", required = true) double cost) {
		bookservice.saveBook(ISBN, title, author, year, publisher, cost);

	}
}
