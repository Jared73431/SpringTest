package com.example.demo.lesson.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 只給第 6 課示範「OFFSET 分頁會漏資料」使用的 Entity */
@Entity
@Table(name = "book_order")
public class BookOrderEntity {

	@Id
	private Long id;

	@Column(name = "book_id")
	private Long bookId;

	private int quantity;

	private String status;

	protected BookOrderEntity() {
	}

	public Long getId() {
		return id;
	}

	public Long getBookId() {
		return bookId;
	}

	public int getQuantity() {
		return quantity;
	}

	public String getStatus() {
		return status;
	}
}
