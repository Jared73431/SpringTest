package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Book;
import com.example.demo.service.BookService;

@RestController
public class BookController {
	@Autowired
	private BookService bookService;

	@GetMapping("/hello")
	public String hello() {
		return "Hello";
	}
	
	/**
	 * 查詢全部資料
	 * 
	 * @return
	 */
	@GetMapping("/findall")
	public List<Book> findall() {
		return bookService.findall();
	}

	/**
	 * 存入一筆資料
	 * 
	 * @param ISBN
	 * @param title
	 * @param author
	 * @param year
	 * @param publisher
	 * @param cost
	 */
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

	/**
	 * 查詢一筆資料
	 * 
	 * @param ID
	 * @return
	 */
	@GetMapping("/getOneBook/{ID}")
	public Book getOneBiik(@PathVariable(value = "ID") int ID) {
		return bookService.findById(ID);
	}

	/**
	 * 修改一筆資料
	 * 
	 * @param ISBN
	 * @param title
	 * @param author
	 * @param year
	 * @param publisher
	 * @param cost
	 */
	@PostMapping("/updateBook")
	public Book updateBook(@RequestParam(value = "ID", required = true) int ID,
			@RequestParam(value = "ISBN", required = true) int ISBN,
			@RequestParam(value = "title", required = true) String title,
			@RequestParam(value = "author", required = true) String author,
			@RequestParam(value = "year", required = true) Integer year,
			@RequestParam(value = "publisher", required = true) String publisher,
			@RequestParam(value = "cost", required = true) double cost) {

		Book upbook = new Book();
		Book book = new Book();
		book.setId(ID);
		book.setISBN(ISBN);
		book.setTitle(title);
		book.setAuthor(author);
		book.setYear(year);
		book.setPublisher(publisher);
		book.setCost(cost);
		upbook = bookService.Update(book);
		return upbook;

	}
}
