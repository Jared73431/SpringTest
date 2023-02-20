package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employee")
public class HelloController {

	@GetMapping("/message")
	public String hello() {
		System.out.println("success");
		return "Hello JavaInUse Called in First Service";
	}
}
