package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.entity.Product;

import lombok.Data;

@Data
public class ProductDTO {
    private String id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private String category;

    // 构造函数
    public ProductDTO() {}

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stock = product.getStock();
        this.description = product.getDescription();
        this.category = product.getCategory();
    }

    // 转换为实体
    public Product toEntity() {
        Product product = new Product();
        product.setId(this.id);
        product.setName(this.name);
        product.setPrice(this.price);
        product.setStock(this.stock);
        product.setDescription(this.description);
        product.setCategory(this.category);
        return product;
    }
}
