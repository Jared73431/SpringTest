package com.example.demo.book;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 書籍的業務邏輯，順便示範自訂的 metrics（第 3 課）：
 *
 * <pre>
 * bookstore.books.added     Counter：建立了幾本書（只會增加）
 * bookstore.books.search    Timer：  搜尋花了多久、搜尋了幾次
 * </pre>
 *
 * HTTP 請求的次數與時間不需要自己寫：Spring Boot 自動記錄在 http.server.requests。
 */
@Service
public class BookService {

	private static final Logger log = LoggerFactory.getLogger(BookService.class);

	private final BookRepository bookRepository;

	private final Counter booksAdded;

	private final Timer searchTimer;

	public BookService(BookRepository bookRepository, MeterRegistry meterRegistry) {
		this.bookRepository = bookRepository;
		this.booksAdded = Counter.builder("bookstore.books.added")
				.description("Number of books created")
				.register(meterRegistry);
		this.searchTimer = Timer.builder("bookstore.books.search")
				.description("Time spent searching books by title")
				.register(meterRegistry);
	}

	public Book create(String title, String author) {
		Book book = bookRepository.insert(title, author);
		booksAdded.increment();
		log.info("Book {} created", book.id());
		return book;
	}

	public Optional<Book> find(long id) {
		return bookRepository.findById(id);
	}

	public List<Book> search(String keyword) {
		// 平常不需要的細節用 DEBUG：線上查問題時，用 /actuator/loggers 暫時打開（第 4 課）
		log.debug("Searching books by title containing '{}'", keyword);
		return searchTimer.record(() -> bookRepository.searchByTitle(keyword));
	}

	public List<Book> findAll() {
		return bookRepository.findAll();
	}
}
