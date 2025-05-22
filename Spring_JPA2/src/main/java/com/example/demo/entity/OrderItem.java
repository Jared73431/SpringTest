package com.example.demo.entity;

import java.math.BigDecimal;

import com.example.demo.entity.compoundKey.OrderItemPK;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "order_item")
public class OrderItem {

    @EmbeddedId
    private OrderItemPK id;

    @ManyToOne
    @MapsId("orderId") // 映射到复合主键的orderId字段
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @ManyToOne
    @MapsId("productId") // 映射到复合主键的productId字段
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // 构造函数
    public OrderItem() {}

    public OrderItem(Order order, Product product, Integer quantity) {
        this.order = order;
        this.product = product;
        this.id = new OrderItemPK(order.getId(), product.getId());
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    // 便捷方法：计算小计金额
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
