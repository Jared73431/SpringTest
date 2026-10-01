package com.example.demo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Book;
import com.example.demo.exception.BookNotFoundException;
import com.example.demo.repository.BookRepo;
import com.example.demo.service.BookService;

@Service
public class BookServiceImpl implements BookService{

	private final BookRepo bookRepo;

	public BookServiceImpl(BookRepo bookRepo) {
		this.bookRepo = bookRepo;
	}
	
	@Override
	public void save(Book book) {
		bookRepo.save(book);
		
	}

	@Override
	public Book findById(Integer id) {
		return bookRepo.findById(id).orElseThrow(() -> new BookNotFoundException(id));
	}

	@Override
	public List<Book> findall() {
		
		return bookRepo.findAll();
	}

	@Override
	@Transactional
	public Book Update(Book book) {
		if (!bookRepo.existsById(book.getId())) {
			throw new BookNotFoundException(book.getId());
		}
		// id 已存在，save 會更新原本那一筆
		return bookRepo.save(book);
	}

	@Override
	public void delete(Integer id) {
		// TODO Auto-generated method stub
		
	}

}
