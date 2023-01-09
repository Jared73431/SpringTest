package com.example.demo.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Table
public class Product implements Persistable {

	@Id
	@Column("ID")
    private Integer id;
	
	@Column("DESCRIPTION")
    private String description;
	
	@Column("PRICE")
    private Double price;

    @Transient
    private boolean newProduct;

    @Override
    @Transient
    public boolean isNew() {
        return this.newProduct || id == null;
    }

    public Product setAsNew() {
        this.newProduct = true;
        return this;
    }
    
}
