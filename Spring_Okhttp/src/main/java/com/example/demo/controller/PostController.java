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

import com.example.demo.client.PostOkHttpClient;
import com.example.demo.model.Post;

/**
 * 貼文 API（/api/posts），透過 OkHttp 同步呼叫 JSONPlaceholder，回傳真正的資料。
 * API 與 Spring_HttpClient 的 /api/posts 相同，可以直接對照 RestClient 與 OkHttp 的寫法。
 * 不寫 try/catch：錯誤由 GlobalExceptionHandler 統一轉成 ProblemDetail。
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

	private final PostOkHttpClient client;

	public PostController(PostOkHttpClient client) {
		this.client = client;
	}

	@GetMapping("/{id}")
	public Post findById(@PathVariable Long id) {
		return client.findById(id);
	}

	@GetMapping
	public List<Post> findAll() {
		return client.findAll();
	}

	@PostMapping
	public ResponseEntity<Post> create(@RequestBody Post post) {
		Post created = client.create(post);
		return ResponseEntity.created(URI.create("/api/posts/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public Post update(@PathVariable Long id, @RequestBody Post post) {
		return client.update(id, post);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		client.delete(id);
		return ResponseEntity.noContent().build();
	}
}
