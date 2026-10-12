package com.example.demo.book;

import java.math.BigDecimal;

/** book 表的一列 */
public record Book(Long id, String isbn, String title, String author, int publishedYear, String publisher,
		BigDecimal price) {
}
