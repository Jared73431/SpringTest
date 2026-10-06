package com.example.demo.web;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Annotated Controller 與 Functional Endpoint 共用的商業邏輯：兩種寫法只是「路由」不同，Service 完全一樣。
 */
@Service
public class GreetingService {

	public Mono<Greeting> greet(String name) {
		return Mono.just(new Greeting("Hello, " + name + "!"));
	}
}
