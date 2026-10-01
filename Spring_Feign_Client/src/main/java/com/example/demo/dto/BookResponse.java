package com.example.demo.dto;

/**
 * Server 回傳的書籍資料。Feign 依這個型別把 JSON 轉成物件；
 * 舊版用 List&lt;?&gt; 接收，每一筆都會變成 LinkedHashMap，無法用 title() 這類方法取值。
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
