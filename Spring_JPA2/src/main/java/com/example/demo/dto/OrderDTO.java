package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;

import lombok.Data;

@Data
public class OrderDTO {

    private String id;
    private String customerId;
    private Date orderDate;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemDTO> items = new ArrayList<>();

    // 构造函数
    public OrderDTO() {}

    public OrderDTO(Order order) {
        this.id = order.getId();
        this.customerId = order.getCustomerId();
        this.orderDate = order.getOrderDate();
        this.status = order.getStatus().name();
        this.totalAmount = order.getTotalAmount();
        this.shippingAddress = order.getShippingAddress();

        // 转换订单项
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                this.items.add(new OrderItemDTO(item));
            }
        }
    }
}
