package com.example.demo.service;

import java.util.List;

import com.example.demo.entity.Book;


public interface BookService {

	Book create(Book book);

	Book findById(Integer id);

	List<Book> findAll();

	Book update(Integer id, Book book);

	void delete(Integer id);
}
