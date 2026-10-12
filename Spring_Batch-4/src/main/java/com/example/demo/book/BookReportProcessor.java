package com.example.demo.book;

import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * 依出版年份分類，並整理書名與出版社的格式。
 *
 * <p>
 * 有自己的商業邏輯，所以寫成實作 ItemProcessor 的類別；沒有邏輯的 Reader、Writer 直接在 JobConfig 用 Builder 建立。
 */
public class BookReportProcessor implements ItemProcessor<Book, BookReportLine> {

	@Override
	public BookReportLine process(Book book) {
		return new BookReportLine(book.isbn(), book.title().trim().toUpperCase(), book.author().trim(),
				book.publishedYear(), book.publisher().trim().toUpperCase(), book.price(), category(book.publishedYear()));
	}

	static String category(int publishedYear) {
		if (publishedYear >= 2020) {
			return "NEW_RELEASE";
		}
		if (publishedYear >= 2010) {
			return "RECENT";
		}
		if (publishedYear >= 2000) {
			return "CLASSIC";
		}
		return "VINTAGE";
	}
}
