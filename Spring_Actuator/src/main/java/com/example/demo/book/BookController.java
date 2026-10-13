package com.example.demo.book;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 被觀察的應用程式：一個簡單的書籍 API。
 *
 * <pre>
 * GET  /api/books               全部，或 ?title= 搜尋
 * GET  /api/books/{id}
 * POST /api/books               {"title", "author"} → 201
 * </pre>
 */
@RestController
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	public record CreateBook(String title, String author) {
	}

	@GetMapping("/api/books")
	public List<Book> list(@RequestParam(required = false) String title) {
		return title == null ? bookService.findAll() : bookService.search(title);
	}

	@GetMapping("/api/books/{id}")
	public Book find(@PathVariable long id) {
		return bookService.find(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book " + id + " not found"));
	}

	@PostMapping("/api/books")
	public ResponseEntity<Book> create(@RequestBody CreateBook request) {
		if (request.title() == null || request.title().isBlank() || request.author() == null
				|| request.author().isBlank()) {
			throw new IllegalArgumentException("title and author are required");
		}
		Book book = bookService.create(request.title(), request.author());
		return ResponseEntity.created(URI.create("/api/books/" + book.id())).body(book);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail badRequest(IllegalArgumentException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}
}
