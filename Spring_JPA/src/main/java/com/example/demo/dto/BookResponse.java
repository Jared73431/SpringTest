package com.example.demo.dto;

import com.example.demo.entity.Book;

/**
 * 書籍的 Response Body。API 不直接回傳 Entity，避免資料庫結構與 API 格式綁在一起。
 */
public record BookResponse(
		Integer id,
		Integer isbn,
		String title,
		String author,
		Integer year,
		String publisher,
		double cost) {

	/** 由 Entity 建立回應物件（靜態工廠方法），轉換邏輯集中在 DTO，Controller 只需呼叫 from() */
	public static BookResponse from(Book book) {
		return new BookResponse(book.getId(), book.getISBN(), book.getTitle(), book.getAuthor(), book.getYear(),
				book.getPublisher(), book.getCost());
	}
}
