package com.example.demo.service;

import java.util.List;

import com.example.demo.entity.Book;

public interface BookService {

	public void save(Book book);
	
	public Book findById(Integer id);
	
	public List<Book> findall();
	
	public void Update(Book book);
	
	public void delete(Integer id);
}
