package com.example.demo.controller;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Jared
 *
 */
@RestController
public class HelloController {

	private static final Logger log = LoggerFactory.getLogger(HelloController.class);

	@GetMapping("/hello")
	public String hello() {
		log.info("hello API 呼叫成功");
		return "Hello World";
	}
	
	public static void main(String[] args) {
		System.out.println("好");

		ArrayList<String> list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
		list.forEach(str -> {
			if (str.length() == 3)
				System.out.println(str);
		});
	}
}
