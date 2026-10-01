package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.example.demo.exception.BusinessRuleViolationException;

/**
 * 商品 Entity，透過 {@link OrderItem} 與 Order 形成多對多關聯。
 * 以 @Version 樂觀鎖保護庫存，避免多筆訂單同時扣庫存時互相覆蓋。
 */
@Entity
@Getter
@Setter
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

    /**
     * 樂觀鎖版本號。
     * 每次更新時 Hibernate 會檢查版本是否與讀取時相同，並把版本 +1；
     * 若期間已被其他交易修改（版本不同），更新失敗並拋出 OptimisticLockException，
     * 避免同時下單時「讀取 → 扣庫存 → 寫回」互相覆蓋（lost update）。
     * default 0：讓既有資料表新增此欄位時，舊資料自動補上版本 0。
     */
    @Version
    @Column(name = "version", nullable = false, columnDefinition = "bigint default 0")
    private Long version;

    // inverse side：外鍵由 OrderItem.product 維護。
    // 刻意不設 cascade：刪除商品不應連帶刪除歷史訂單項（@OneToMany 預設為 LAZY）
    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();

    // 建構子
    public Product() {}

    public Product(String id, String name, BigDecimal price, Integer stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // 便利方法：減少庫存
    // 業務規則放在 Entity 內（而非 Service 直接 setStock），庫存檢查集中在一處，呼叫端不必各自判斷
    public void reduceStock(int quantity) {
        if (this.stock < quantity) {
            throw new BusinessRuleViolationException("庫存不足");
        }
        this.stock -= quantity;
    }
}
