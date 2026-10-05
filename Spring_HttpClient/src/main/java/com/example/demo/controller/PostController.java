package com.example.demo.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.client.PostRestClient;
import com.example.demo.model.Post;

/**
 * 貼文 API（/api/posts），透過 {@link PostRestClient}（同步）轉呼叫 JSONPlaceholder。
 * 與 {@link ReactivePostController} 功能相同，方便對照兩種 HTTP client 的寫法。
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

	private final PostRestClient postRestClient;

	public PostController(PostRestClient postRestClient) {
		this.postRestClient = postRestClient;
	}

	@GetMapping("/{id}")
	public Post findById(@PathVariable Long id) {
		return postRestClient.findById(id);
	}

	@GetMapping
	public List<Post> findAll() {
		return postRestClient.findAll();
	}

	@PostMapping
	public ResponseEntity<Post> create(@RequestBody Post post) {
		Post created = postRestClient.create(post);
		return ResponseEntity.created(URI.create("/api/posts/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public Post update(@PathVariable Long id, @RequestBody Post post) {
		return postRestClient.update(id, post);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		postRestClient.delete(id);
		return ResponseEntity.noContent().build();
	}
}
