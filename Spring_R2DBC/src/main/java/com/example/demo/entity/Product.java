package com.example.demo.entity;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC 的 Entity 只是「一列資料」的對應：沒有 JPA 的 lazy loading、dirty checking，也沒有 @OneToMany 之類的關聯。
 *
 * <p>
 * id 由資料庫產生（SERIAL）：id 為 null 時 save() 執行 INSERT，有值時執行 UPDATE。這種情況不需要實作 Persistable
 * （修正前實作了 Persistable，isNew() 還被 Jackson 當成 getter，回應多出 new、newProduct 兩個欄位）。
 */
@Table("product")
public class Product {

	@Id
	private Integer id;

	private String description;

	private BigDecimal price;

	public Product() {
	}

	public Product(String description, BigDecimal price) {
		this.description = description;
		this.price = price;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}
}
