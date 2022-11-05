package com.example.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Book;
import com.example.demo.repository.BookRepo;
import com.example.demo.service.BookService;

@Service
public class BookServiceImpl implements BookService{

	@Autowired
	private BookRepo bookRepo;
	
	@Override
	public void save(Book book) {
		bookRepo.save(book);
		
	}

	@Override
	public Book findById(Integer id) {
		
		return bookRepo.findById(id).orElseThrow();
	}

	@Override
	public List<Book> findall() {
		
		return bookRepo.findAll();
	}

	@Override
	public Book Update(Book book) {
		Book upbook = new Book();
		if (bookRepo.existsById(book.getId())) {
			book.setId(null);
			bookRepo.save(book);
			upbook = book;
		} else {
			upbook = bookRepo.saveAndFlush(book);
		}
		
		return upbook;
		
	}

	@Override
	public void delete(Integer id) {
		// TODO Auto-generated method stub
		
	}

}
