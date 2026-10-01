package com.example.demo.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.compoundKey.OrderItemPK;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
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
        // 先檢查所有數量，避免負數數量反而增加庫存
        productQuantities.forEach(this::requirePositiveQuantity);

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
                    .orElseThrow(() -> new InvalidRequestException("商品不存在: " + productId));

            // 檢查庫存
            if (product.getStock() < quantity) {
                throw new BusinessRuleViolationException("商品庫存不足: " + product.getName());
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

        // 依狀態機檢查是否可以取消（已取消的訂單不能再取消，避免重複補回庫存）
        if (!order.getStatus().canTransitionTo(Order.OrderStatus.CANCELLED)) {
            throw new BusinessRuleViolationException("訂單狀態為 " + order.getStatus() + "，無法取消");
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

        // 取消需要補回庫存，只能透過取消訂單 API
        if (status == Order.OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("取消訂單請使用 POST /api/orders/{id}/cancel，才會補回庫存");
        }
        if (!order.getStatus().canTransitionTo(status)) {
            throw new BusinessRuleViolationException(
                    "訂單狀態無法從 " + order.getStatus() + " 變更為 " + status);
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public Order addOrderItem(String orderId, String productId, int quantity) {
        requirePositiveQuantity(productId, quantity);
        Order order = findOrder(orderId);

        // 檢查訂單狀態
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessRuleViolationException("只能修改待處理狀態的訂單");
        }

        // 查找商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new InvalidRequestException("商品不存在: " + productId));

        // 檢查庫存
        if (product.getStock() < quantity) {
            throw new BusinessRuleViolationException("商品庫存不足: " + product.getName());
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
            throw new BusinessRuleViolationException("只能修改待處理狀態的訂單");
        }

        // 查找訂單項
        OrderItemPK pk = new OrderItemPK(orderId, productId);
        OrderItem item = orderItemRepository.findById(pk)
                .orElseThrow(() -> new ResourceNotFoundException("訂單項", productId));

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
                .orElseThrow(() -> new ResourceNotFoundException("訂單", orderId));
    }

    @Override
    public List<Order> findCustomerOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    private void requirePositiveQuantity(String productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidRequestException("商品數量必須大於 0: " + productId);
        }
    }
}
