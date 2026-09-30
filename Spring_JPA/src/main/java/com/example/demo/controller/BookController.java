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

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.example.demo.entity.Book;
import com.example.demo.service.BookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	/**
	 * 查詢全部資料
	 */
	@GetMapping
	public List<BookResponse> findAll() {
		return bookService.findAll().stream().map(BookResponse::from).toList();
	}

	/**
	 * 查詢一筆資料，不存在時回 404
	 */
	@GetMapping("/{id}")
	public BookResponse findById(@PathVariable Integer id) {
		return BookResponse.from(bookService.findById(id));
	}

	/**
	 * 新增一筆資料，回 201 並在 Location header 帶上新資料的網址
	 */
	@PostMapping
	public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
		Book created = bookService.create(request.toEntity());
		return ResponseEntity.created(URI.create("/api/books/" + created.getId())).body(BookResponse.from(created));
	}

	/**
	 * 修改一筆資料，不存在時回 404
	 */
	@PutMapping("/{id}")
	public BookResponse update(@PathVariable Integer id, @Valid @RequestBody BookRequest request) {
		return BookResponse.from(bookService.update(id, request.toEntity()));
	}

	/**
	 * 刪除一筆資料，成功回 204，不存在時回 404
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		bookService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
