package com.example.demo.entity.compoundKey;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OrderItem 的複合主鍵 (order_id, product_id)，以 @Embeddable 嵌入 OrderItem 的 @EmbeddedId。
 * JPA 規範要求複合主鍵類別必須實作 Serializable、提供無參數建構子，
 * 並正確覆寫 equals / hashCode（Hibernate 以此在 Persistence Context 中判斷是否為同一筆資料）。
 */
@Embeddable
@Data
@EqualsAndHashCode
public class OrderItemPK implements Serializable {

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    // 建構子
    public OrderItemPK() {}

    public OrderItemPK(String orderId, String productId) {
        this.orderId = orderId;
        this.productId = productId;
    }
}
