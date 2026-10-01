package com.example.demo.entity;

import java.math.BigDecimal;

import com.example.demo.entity.compoundKey.OrderItemPK;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "order_item")
public class OrderItem {

    @EmbeddedId
    private OrderItemPK id;

    @ManyToOne
    @MapsId("orderId") // 映射到複合主鍵的orderId欄位
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @ManyToOne
    @MapsId("productId") // 映射到複合主鍵的productId欄位
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // 建構子
    public OrderItem() {}

    public OrderItem(Order order, Product product, Integer quantity) {
        this.order = order;
        this.product = product;
        this.id = new OrderItemPK(order.getId(), product.getId());
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    // 便利方法：計算小計金額
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
