package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.entity.Product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ProductDTO {
    @Size(max = 36, message = "商品 ID 最多 36 字")
    private String id;

    @NotBlank(message = "商品名稱不可為空")
    @Size(max = 100, message = "商品名稱最多 100 字")
    private String name;

    @NotNull(message = "價格不可為空")
    @PositiveOrZero(message = "價格不可為負數")
    private BigDecimal price;

    @NotNull(message = "庫存不可為空")
    @PositiveOrZero(message = "庫存不可為負數")
    private Integer stock;

    @Size(max = 500, message = "商品描述最多 500 字")
    private String description;

    @Size(max = 50, message = "商品類別最多 50 字")
    private String category;

    // 建構子
    public ProductDTO() {}

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stock = product.getStock();
        this.description = product.getDescription();
        this.category = product.getCategory();
    }

    // 轉換為實體
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
