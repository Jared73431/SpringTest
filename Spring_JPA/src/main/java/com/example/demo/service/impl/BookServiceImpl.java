package com.example.demo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Book;
import com.example.demo.exception.BookNotFoundException;
import com.example.demo.repository.BookRepo;
import com.example.demo.service.BookService;

@Service
public class BookServiceImpl implements BookService {

	private final BookRepo bookRepo;

	public BookServiceImpl(BookRepo bookRepo) {
		this.bookRepo = bookRepo;
	}

	@Override
	public Book create(Book book) {
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

	@Override
	@Transactional
	public Book update(Integer id, Book book) {
		if (!bookRepo.existsById(id)) {
			throw new BookNotFoundException(id);
		}
		// id 已存在，save 會更新原本那一筆
		book.setId(id);
		return bookRepo.save(book);
	}

	@Override
	@Transactional
	public void delete(Integer id) {
		if (!bookRepo.existsById(id)) {
			throw new BookNotFoundException(id);
		}
		bookRepo.deleteById(id);
	}

}
