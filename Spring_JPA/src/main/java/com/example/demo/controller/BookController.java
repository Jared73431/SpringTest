package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Book;
import com.example.demo.service.BookService;

@RestController
public class BookController {

	@Autowired
	private BookService bookService;
	
	@GetMapping("/findall")
	public List<Book> findall(){
		return bookService.findall();
	}
	
	@PostMapping("/saveBook")
	public void saveBook(@RequestParam(value = "ISBN", required = true) int ISBN,
			@RequestParam(value = "title", required = true) String title,
			@RequestParam(value = "author", required = true) String author,
			@RequestParam(value = "year", required = true) Integer year,
			@RequestParam(value = "publisher", required = true) String publisher,
			@RequestParam(value = "cost", required = true) double cost) {
		Book book = new Book();
		book.setISBN(ISBN);
		book.setTitle(title);
		book.setAuthor(author);
		book.setYear(year);
		book.setPublisher(publisher);
		book.setCost(cost);
		bookService.save(book);
		
	}
}
