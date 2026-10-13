package com.example.demo.dto;

/**
 * Server 回傳的書籍資料。HTTP Service Client 依方法的回傳型別把 JSON 轉成物件。
 */
public record BookResponse(
		Integer id,
		Integer isbn,
		String title,
		String author,
		Integer year,
		String publisher,
		double cost) {
}
