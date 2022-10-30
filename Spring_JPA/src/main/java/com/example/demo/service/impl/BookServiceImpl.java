package com.example.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.entity.Book;
import com.example.demo.repository.BookRepo;
import com.example.demo.service.BookService;

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
	public void Update(Book book) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Integer id) {
		// TODO Auto-generated method stub
		
	}

}
