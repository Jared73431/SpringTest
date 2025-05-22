package com.example.demo.entity.compoundKey;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Embeddable
@Data
@EqualsAndHashCode
public class OrderItemPK implements Serializable {

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    // 构造函数
    public OrderItemPK() {}

    public OrderItemPK(String orderId, String productId) {
        this.orderId = orderId;
        this.productId = productId;
    }
}
