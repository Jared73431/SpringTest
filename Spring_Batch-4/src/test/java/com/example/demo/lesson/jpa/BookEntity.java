package com.example.demo.lesson.jpa;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 只給第 1 課比較 JpaPagingItemReader、RepositoryItemReader 使用的 Entity（主程式全部使用 JDBC，沒有 JPA）。
 */
@Entity
@Table(name = "book")
public class BookEntity {

	@Id
	private Long id;

	private String isbn;

	private String title;

	@Column(name = "published_year")
	private int publishedYear;

	private BigDecimal price;

	protected BookEntity() {
	}

	public Long getId() {
		return id;
	}

	public String getIsbn() {
		return isbn;
	}

	public String getTitle() {
		return title;
	}

	public int getPublishedYear() {
		return publishedYear;
	}

	public BigDecimal getPrice() {
		return price;
	}
}
