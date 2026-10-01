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

import com.example.demo.client.BookClient;
import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;

/**
 * 書籍 API（/api/books），每個端點都透過 {@link BookClient} 轉呼叫 Spring_Feign_Server 的同名端點。
 * 不寫 try/catch：Server 回傳的錯誤由 BookServerErrorDecoder 轉成例外，再由 GlobalExceptionHandler 統一處理。
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

	private final BookClient bookClient;

	public BookController(BookClient bookClient) {
		this.bookClient = bookClient;
	}

	@GetMapping
	public List<BookResponse> findAll() {
		return bookClient.findAll();
	}

	@GetMapping("/{id}")
	public BookResponse findById(@PathVariable Integer id) {
		return bookClient.findById(id);
	}

	/**
	 * 回 201，Location 指向本服務（Client）的網址，而不是 Server 的網址
	 */
	@PostMapping
	public ResponseEntity<BookResponse> create(@RequestBody BookRequest request) {
		BookResponse created = bookClient.create(request);
		return ResponseEntity.created(URI.create("/api/books/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public BookResponse update(@PathVariable Integer id, @RequestBody BookRequest request) {
		return bookClient.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		bookClient.delete(id);
		return ResponseEntity.noContent().build();
	}
}
