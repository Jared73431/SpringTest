package com.example.demo.service;

import java.util.List;

import com.example.demo.entity.Book;


/**
 * 書籍的商業邏輯介面，實作在 {@link com.example.demo.service.impl.BookServiceImpl}。
 * 查不到資料時一律拋出 BookNotFoundException（由 GlobalExceptionHandler 轉成 404）。
 */
public interface BookService {

	/** 新增書籍，id 由資料庫 sequence 產生 */
	Book create(Book book);

	/** 依 id 查詢，不存在時拋出 BookNotFoundException */
	Book findById(Integer id);

	List<Book> findAll();

	/** 更新指定 id 的書籍，不存在時拋出 BookNotFoundException */
	Book update(Integer id, Book book);

	/** 刪除指定 id 的書籍，不存在時拋出 BookNotFoundException */
	void delete(Integer id);
}
