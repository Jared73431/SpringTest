package com.example.demo.book;

import java.math.BigDecimal;

/** 書籍報表（CSV）的一行 */
public record BookReportLine(String isbn, String title, String author, int publishedYear, String publisher,
		BigDecimal price, String category) {
}
