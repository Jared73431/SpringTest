package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.entity.OrderItem;

import lombok.Data;

/**
 * 訂單項的回應 DTO：只取出商品 ID 與名稱，不帶出整個 Product Entity，
 * 並把 OrderItem.getSubtotal() 的計算結果一併輸出，前端不必自行計算。
 */
@Data
public class OrderItemDTO {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // 建構子
    public OrderItemDTO() {}

    public OrderItemDTO(OrderItem orderItem) {
        this.productId = orderItem.getProduct().getId();
        this.productName = orderItem.getProduct().getName();
        this.quantity = orderItem.getQuantity();
        this.unitPrice = orderItem.getUnitPrice();
        this.subtotal = orderItem.getSubtotal();
    }
}
