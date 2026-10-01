package com.example.demo.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 書籍的 JPA Entity，對應資料表 book（資料表由 Hibernate 依 ddl-auto=update 自動建立）。
 * 只用 {@code @Getter} / {@code @Setter}，不用 {@code @Data}：
 * {@code @Data} 產生的 equals / hashCode 使用所有欄位，Entity 放入 Set 或 id 改變時容易出錯。
 * API 不直接回傳這個類別，而是轉成 BookResponse（見 dto 套件）。
 */
@Getter
@Setter
@Entity
@ToString
public class Book implements Serializable{

	/** Serializable 的版本號，物件序列化格式改變時用來辨識相容性 */
	private static final long serialVersionUID = -3392162000827962466L;

	/**
	 * 主鍵由 PostgreSQL 的 sequence（book_id_seq）產生。
	 * allocationSize = 1：每次新增都向資料庫取號，與 sequence 每次 +1 的設定一致。
	 */
	@Id
	@SequenceGenerator(name="Book_id_GENERATOR", sequenceName="Book_id_seq", allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Book_id_GENERATOR")
	private Integer Id;

	// 練習用設計：Integer 存不下 13 碼的 ISBN（見 readme「已知限制」）
	private Integer ISBN;

	private String title;

	private String author;

	private Integer year;

	private String publisher;

	// 練習用設計：金額用 double 可能有浮點誤差，實務上建議 BigDecimal
	private double cost;
}
