package com.example.demo.controller;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.OrderDTO;
import com.example.demo.entity.Order;
import com.example.demo.request.AddOrderItemRequest;
import com.example.demo.request.CreateOrderRequest;
import com.example.demo.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 根据ID获取订单
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String id) {
        try {
            Order order = orderService.findOrder(id);
            return ResponseEntity.ok(new OrderDTO(order));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 获取客户所有订单
    @GetMapping("/customer/{customerId}")
    public List<OrderDTO> getCustomerOrders(@PathVariable String customerId) {
        return orderService.findCustomerOrders(customerId).stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
    }

    // 创建订单
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Order order = orderService.createOrder(
                    request.getCustomerId(),
                    request.getShippingAddress(),
                    request.getProductQuantities());

            return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                    .body(new OrderDTO(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 取消订单
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable String id) {
        try {
            Order order = orderService.cancelOrder(id);
            return ResponseEntity.ok(new OrderDTO(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 更新订单状态
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable String id,
            @RequestParam Order.OrderStatus status) {
        try {
            Order order = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(new OrderDTO(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 添加订单项
    @PostMapping("/{id}/items")
    public ResponseEntity<OrderDTO> addOrderItem(
            @PathVariable String id,
            @RequestBody AddOrderItemRequest request) {
        try {
            Order order = orderService.addOrderItem(id, request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(new OrderDTO(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 移除订单项
    @DeleteMapping("/{orderId}/items/{productId}")
    public ResponseEntity<OrderDTO> removeOrderItem(
            @PathVariable String orderId,
            @PathVariable String productId) {
        try {
            Order order = orderService.removeOrderItem(orderId, productId);
            return ResponseEntity.ok(new OrderDTO(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
