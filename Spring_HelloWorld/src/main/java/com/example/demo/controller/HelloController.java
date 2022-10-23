package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * @author Jared
 *
 */
@RestController
public class HelloController {

	@GetMapping("/hello")
	public String hello() {
		System.out.println("成功");
		return "Hello World";
	}
}
