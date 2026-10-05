package com.example.demo.exception;

public class BookNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BookNotFoundException(Integer id) {
		super("Book not found: id=" + id);
	}
}
