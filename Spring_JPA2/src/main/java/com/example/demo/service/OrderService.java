package com.example.demo.service;

import java.util.List;
import java.util.Map;

import com.example.demo.entity.Order;

public interface OrderService {

    // 建立訂單
    Order createOrder(String customerId, String shippingAddress, Map<String, Integer> productQuantities);

    // 取消訂單
    Order cancelOrder(String orderId);

    // 更新訂單狀態
    Order updateOrderStatus(String orderId, Order.OrderStatus status);

    // 新增訂單項
    Order addOrderItem(String orderId, String productId, int quantity);

    // 移除訂單項
    Order removeOrderItem(String orderId, String productId);

    // 查找訂單
    Order findOrder(String orderId);

    // 查找客戶所有訂單
    List<Order> findCustomerOrders(String customerId);
}
