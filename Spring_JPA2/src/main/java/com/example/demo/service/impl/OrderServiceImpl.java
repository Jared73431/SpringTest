package com.example.demo.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.compoundKey.OrderItemPK;
import com.example.demo.entity.Product;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.OrderService;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final ProductRepository productRepository;

    private final OrderItemRepository orderItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public Order createOrder(String customerId, String shippingAddress, Map<String, Integer> productQuantities) {
        // 生成訂單ID
        String orderId = UUID.randomUUID().toString();

        // 建立訂單物件
        Order order = new Order(orderId, customerId);
        order.setShippingAddress(shippingAddress);

        // 保存訂單
        order = orderRepository.save(order);

        // 新增訂單項
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productId = entry.getKey();
            Integer quantity = entry.getValue();

            // 查找商品
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + productId));

            // 檢查庫存
            if (product.getStock() < quantity) {
                throw new RuntimeException("商品庫存不足: " + product.getName());
            }

            // 建立訂單項
            OrderItem orderItem = new OrderItem(order, product, quantity);
            order.addItem(orderItem);

            // 減少商品庫存
            product.reduceStock(quantity);
            productRepository.save(product);
        }

        // 重新計算訂單總金額
        order.recalculateTotalAmount();

        // 保存更新後的訂單
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(String orderId) {
        Order order = findOrder(orderId);

        // 檢查訂單狀態是否可以取消
        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("訂單已發貨或已交付，無法取消");
        }

        // 更新訂單狀態
        order.setStatus(Order.OrderStatus.CANCELLED);

        // 恢復商品庫存
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        return orderRepository.save(order);
    }

    @Override
    public Order updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = findOrder(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public Order addOrderItem(String orderId, String productId, int quantity) {
        Order order = findOrder(orderId);

        // 檢查訂單狀態
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("只能修改待處理狀態的訂單");
        }

        // 查找商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在: " + productId));

        // 檢查庫存
        if (product.getStock() < quantity) {
            throw new RuntimeException("商品庫存不足: " + product.getName());
        }

        // 檢查訂單是否已包含該商品
        OrderItemPK pk = new OrderItemPK(orderId, productId);
        Optional<OrderItem> existingItem = orderItemRepository.findById(pk);

        if (existingItem.isPresent()) {
            // 更新已有訂單項的數量
            OrderItem item = existingItem.get();
            // 先恢復原來的庫存
            product.setStock(product.getStock() + item.getQuantity());
            // 設置新數量
            item.setQuantity(quantity);
            // 再減去新的庫存
            product.reduceStock(quantity);
            productRepository.save(product);
        } else {
            // 建立新的訂單項
            OrderItem newItem = new OrderItem(order, product, quantity);
            order.addItem(newItem);
            // 減少商品庫存
            product.reduceStock(quantity);
            productRepository.save(product);
        }

        // 重新計算訂單總金額
        order.recalculateTotalAmount();

        return orderRepository.save(order);
    }

    @Override
    public Order removeOrderItem(String orderId, String productId) {
        Order order = findOrder(orderId);

        // 檢查訂單狀態
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("只能修改待處理狀態的訂單");
        }

        // 查找訂單項
        OrderItemPK pk = new OrderItemPK(orderId, productId);
        OrderItem item = orderItemRepository.findById(pk)
                .orElseThrow(() -> new RuntimeException("訂單項不存在"));

        // 恢復商品庫存
        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
        productRepository.save(product);

        // 移除訂單項
        order.removeItem(item);
        orderItemRepository.delete(item);

        // 重新計算訂單總金額
        order.recalculateTotalAmount();

        return orderRepository.save(order);
    }

    @Override
    public Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("訂單不存在: " + orderId));
    }

    @Override
    public List<Order> findCustomerOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
