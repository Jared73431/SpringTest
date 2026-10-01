package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;

import lombok.Data;

/**
 * 訂單的回應 DTO：把 Order Entity 攤平成 API 需要的格式（status 轉為字串，訂單項轉為 OrderItemDTO）。
 * 不直接回傳 Entity，可避免 Order ↔ OrderItem ↔ Product 的雙向關聯在序列化時無限遞迴。
 */
@Data
public class OrderDTO {

    private String id;
    private String customerId;
    private Date orderDate;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemDTO> items = new ArrayList<>();

    // 建構子
    // 無參數建構子供 Jackson 反序列化使用
    public OrderDTO() {}

    // 會走訪 items 與每個 item 的 product，需在交易內（Entity 仍為 managed 狀態）呼叫
    public OrderDTO(Order order) {
        this.id = order.getId();
        this.customerId = order.getCustomerId();
        this.orderDate = order.getOrderDate();
        this.status = order.getStatus().name();
        this.totalAmount = order.getTotalAmount();
        this.shippingAddress = order.getShippingAddress();

        // 轉換訂單項
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                this.items.add(new OrderItemDTO(item));
            }
        }
    }
}
