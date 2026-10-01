package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "orders") // 使用"orders"而不是"order"，因為"order"是SQL關鍵字
public class Order {

    @Id
    @Column(name = "order_id", length = 36)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "order_date", nullable = false)
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

    // 訂單狀態列舉
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    // 建構子
    public Order() {
        this.orderDate = new Date();
        this.status = OrderStatus.PENDING;
    }

    public Order(String id, String customerId) {
        this();
        this.id = id;
        this.customerId = customerId;
    }

    // 便利方法：新增訂單項
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);

        // 重新計算訂單總金額
        recalculateTotalAmount();
    }

    // 便利方法：移除訂單項
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);

        // 重新計算訂單總金額
        recalculateTotalAmount();
    }

    // 便利方法：計算訂單總金額
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
