package com.example.demo.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.client.PostWebClient;
import com.example.demo.model.Post;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 貼文 API（/api/reactive/posts），透過 {@link PostWebClient}（Reactive）轉呼叫 JSONPlaceholder。
 * 本應用程式是 Spring MVC：回傳 Mono / Flux 時，Spring MVC 以非同步請求處理並在完成後寫出回應，
 * 但伺服器本身仍是 Tomcat（Servlet），並不會因此變成「全 Reactive」的應用程式。
 */
@RestController
@RequestMapping("/api/reactive/posts")
public class ReactivePostController {

	private final PostWebClient postWebClient;

	public ReactivePostController(PostWebClient postWebClient) {
		this.postWebClient = postWebClient;
	}

	@GetMapping("/{id}")
	public Mono<Post> findById(@PathVariable Long id) {
		return postWebClient.findById(id);
	}

	@GetMapping
	public Flux<Post> findAll() {
		return postWebClient.findAll();
	}

	@PostMapping
	public Mono<ResponseEntity<Post>> create(@RequestBody Post post) {
		return postWebClient.create(post)
				.map(created -> ResponseEntity.created(URI.create("/api/reactive/posts/" + created.id())).body(created));
	}

	@PutMapping("/{id}")
	public Mono<Post> update(@PathVariable Long id, @RequestBody Post post) {
		return postWebClient.update(id, post);
	}

	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
		return postWebClient.delete(id).then(Mono.just(ResponseEntity.noContent().build()));
	}
}
