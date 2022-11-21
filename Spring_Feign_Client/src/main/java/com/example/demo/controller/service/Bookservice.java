package com.example.demo.controller.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "aac", url = "http://localhost:8082")
public interface Bookservice {

    @GetMapping("/hello")
    public String hello();
    
    @GetMapping("/findall")
    public List<?> findall();
    
    @PostMapping("/saveBook")
    public void saveBook(@RequestParam(value = "ISBN", required = true) int ISBN,
    		@RequestParam(value = "title", required = true) String title,
    		@RequestParam(value = "author", required = true) String author,
    		@RequestParam(value = "year", required = true) Integer year,
			@RequestParam(value = "publisher", required = true) String publisher,
			@RequestParam(value = "cost", required = true) double cost);
}
