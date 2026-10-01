package com.example.demo.controller;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

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

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 根據ID取得訂單
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String id) {
        Order order = orderService.findOrder(id);
        return ResponseEntity.ok(new OrderDTO(order));
    }

    // 取得客戶所有訂單
    @GetMapping("/customer/{customerId}")
    public List<OrderDTO> getCustomerOrders(@PathVariable String customerId) {
        return orderService.findCustomerOrders(customerId).stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
    }

    // 建立訂單
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(
                request.getCustomerId(),
                request.getShippingAddress(),
                request.getProductQuantities());

        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(new OrderDTO(order));
    }

    // 取消訂單
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable String id) {
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(new OrderDTO(order));
    }

    // 更新訂單狀態
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable String id,
            @RequestParam Order.OrderStatus status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(new OrderDTO(order));
    }

    // 新增訂單項
    @PostMapping("/{id}/items")
    public ResponseEntity<OrderDTO> addOrderItem(
            @PathVariable String id,
            @RequestBody AddOrderItemRequest request) {
        Order order = orderService.addOrderItem(id, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(new OrderDTO(order));
    }

    // 移除訂單項
    @DeleteMapping("/{orderId}/items/{productId}")
    public ResponseEntity<OrderDTO> removeOrderItem(
            @PathVariable String orderId,
            @PathVariable String productId) {
        Order order = orderService.removeOrderItem(orderId, productId);
        return ResponseEntity.ok(new OrderDTO(order));
    }
}
