package com.example.demo.service;

import java.util.List;
import java.util.Map;

import com.example.demo.entity.Order;

public interface OrderService {

    // 创建订单
    Order createOrder(String customerId, String shippingAddress, Map<String, Integer> productQuantities);

    // 取消订单
    Order cancelOrder(String orderId);

    // 更新订单状态
    Order updateOrderStatus(String orderId, Order.OrderStatus status);

    // 添加订单项
    Order addOrderItem(String orderId, String productId, int quantity);

    // 移除订单项
    Order removeOrderItem(String orderId, String productId);

    // 查找订单
    Order findOrder(String orderId);

    // 查找客户所有订单
    List<Order> findCustomerOrders(String customerId);
}
