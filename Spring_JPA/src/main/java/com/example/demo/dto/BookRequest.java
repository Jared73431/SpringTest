package com.example.demo.dto;

import com.example.demo.entity.Book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 新增 / 修改書籍的 Request Body。所有欄位皆為必填。
 */
public record BookRequest(
		@NotNull Integer isbn,
		@NotBlank String title,
		@NotBlank String author,
		@NotNull Integer year,
		@NotBlank String publisher,
		@NotNull Double cost) {

	public Book toEntity() {
		Book book = new Book();
		book.setISBN(isbn);
		book.setTitle(title);
		book.setAuthor(author);
		book.setYear(year);
		book.setPublisher(publisher);
		book.setCost(cost);
		return book;
	}
}
