package com.example.demo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * 寫法一：Annotated Controller。和 Spring MVC 幾乎一樣，差別只在回傳 Mono / Flux。
 *
 * <p>
 * 回傳 Mono 之後，由 WebFlux 訂閱它並把結果寫進 HTTP 回應（第 1 課：沒有人訂閱就什麼都不會發生，這裡訂閱的人是框架）。
 */
@RestController
@RequestMapping("/api/greetings")
public class GreetingController {

	private final GreetingService greetingService;

	public GreetingController(GreetingService greetingService) {
		this.greetingService = greetingService;
	}

	@GetMapping("/{name}")
	public Mono<Greeting> greet(@PathVariable String name) {
		return greetingService.greet(name);
	}
}
