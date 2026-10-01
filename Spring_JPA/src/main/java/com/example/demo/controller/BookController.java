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

/**
 * 書籍 REST API（/api/books）。
 * Controller 只負責 HTTP 與 DTO 轉換（BookRequest → Entity → BookResponse），商業邏輯在 BookService。
 * 不寫 try/catch：Service 拋出的 BookNotFoundException 由 GlobalExceptionHandler 統一轉成 404 ProblemDetail；
 * {@code @Valid} 驗證失敗由 Spring 回 400。
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	/**
	 * GET /api/books：查詢全部資料
	 */
	@GetMapping
	public List<BookResponse> findAll() {
		return bookService.findAll().stream().map(BookResponse::from).toList();
	}

	/**
	 * GET /api/books/{id}：查詢一筆資料，不存在時回 404
	 */
	@GetMapping("/{id}")
	public BookResponse findById(@PathVariable Integer id) {
		return BookResponse.from(bookService.findById(id));
	}

	/**
	 * POST /api/books：新增一筆資料，回 201 並在 Location header 帶上新資料的網址；欄位驗證失敗回 400
	 */
	@PostMapping
	public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
		Book created = bookService.create(request.toEntity());
		return ResponseEntity.created(URI.create("/api/books/" + created.getId())).body(BookResponse.from(created));
	}

	/**
	 * PUT /api/books/{id}：修改一筆資料，不存在時回 404；欄位驗證失敗回 400
	 */
	@PutMapping("/{id}")
	public BookResponse update(@PathVariable Integer id, @Valid @RequestBody BookRequest request) {
		return BookResponse.from(bookService.update(id, request.toEntity()));
	}

	/**
	 * DELETE /api/books/{id}：刪除一筆資料，成功回 204，不存在時回 404
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		bookService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
