package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.entity.Product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 商品的 DTO，同時作為新增 / 更新的 Request 與查詢的 Response。
 * 欄位上的 Bean Validation 限制與 Product Entity 的 @Column 長度一致，讓錯誤在進入資料庫前就以 400 回應。
 * 刻意不包含 version 與 orderItems，避免把樂觀鎖與關聯細節暴露給 API。
 */
@Data
public class ProductDTO {
    // id 可省略（不加 @NotBlank）：未提供時由 Service 產生 UUID
    @Size(max = 36, message = "商品 ID 最多 36 字")
    private String id;

    @NotBlank(message = "商品名稱不可為空")
    @Size(max = 100, message = "商品名稱最多 100 字")
    private String name;

    // @PositiveOrZero 對 null 視為通過，因此需要再搭配 @NotNull 才能要求必填
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
