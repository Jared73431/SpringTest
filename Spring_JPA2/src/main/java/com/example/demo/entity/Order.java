package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "orders") // 使用"orders"而不是"order"，因为"order"是SQL关键字
public class Order {

    @Id
    @Column(name = "order_id", length = 36)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "order_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_address", length = 255)
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // 订单状态枚举
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    // 构造函数
    public Order() {
        this.orderDate = new Date();
        this.status = OrderStatus.PENDING;
    }

    public Order(String id, String customerId) {
        this();
        this.id = id;
        this.customerId = customerId;
    }

    // 便捷方法：添加订单项
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);

        // 重新计算订单总金额
        recalculateTotalAmount();
    }

    // 便捷方法：移除订单项
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);

        // 重新计算订单总金额
        recalculateTotalAmount();
    }

    // 便捷方法：计算订单总金额
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
