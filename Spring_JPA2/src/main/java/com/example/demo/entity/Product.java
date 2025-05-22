package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "product")
public class Product {

    @Id
    @Column(name = "product_id", length = 36)
    private String id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();

    // 构造函数
    public Product() {}

    public Product(String id, String name, BigDecimal price, Integer stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // 便捷方法：减少库存
    public void reduceStock(int quantity) {
        if (this.stock < quantity) {
            throw new RuntimeException("库存不足");
        }
        this.stock -= quantity;
    }
}
