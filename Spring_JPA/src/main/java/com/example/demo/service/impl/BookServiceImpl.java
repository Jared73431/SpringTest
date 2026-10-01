package com.example.demo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Book;
import com.example.demo.exception.BookNotFoundException;
import com.example.demo.repository.BookRepo;
import com.example.demo.service.BookService;

/**
 * {@link BookService} 的實作。
 * 使用 Constructor Injection：依賴宣告為 final、一眼就能看出需要哪些元件，也能不啟動 Spring 直接撰寫單元測試。
 */
@Service
public class BookServiceImpl implements BookService {

	private final BookRepo bookRepo;

	// 只有一個建構子時，Spring 會自動用它注入依賴，不需要加 @Autowired
	public BookServiceImpl(BookRepo bookRepo) {
		this.bookRepo = bookRepo;
	}

	@Override
	public Book create(Book book) {
		// 確保 id 為 null：save() 會判斷為新資料而 INSERT，id 由 sequence 產生
		book.setId(null);
		return bookRepo.save(book);
	}

	@Override
	public Book findById(Integer id) {
		return bookRepo.findById(id).orElseThrow(() -> new BookNotFoundException(id));
	}

	@Override
	public List<Book> findAll() {
		return bookRepo.findAll();
	}

	// @Transactional：「檢查是否存在」與「儲存」在同一個交易中執行
	@Override
	@Transactional
	public Book update(Integer id, Book book) {
		if (!bookRepo.existsById(id)) {
			throw new BookNotFoundException(id);
		}
		// id 已存在，save 會更新原本那一筆
		// （修正前的寫法會先把 id 設為 null 再存，結果每次「更新」都新增一筆重複資料）
		book.setId(id);
		return bookRepo.save(book);
	}

	@Override
	@Transactional
	public void delete(Integer id) {
		// Spring Data 3 起 deleteById 遇到不存在的 id 不會拋例外，所以先檢查，才能回 404
		if (!bookRepo.existsById(id)) {
			throw new BookNotFoundException(id);
		}
		bookRepo.deleteById(id);
	}

}
