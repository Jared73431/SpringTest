package com.example.demo.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Override
    public Order createOrder(String customerId, String shippingAddress, Map<String, Integer> productQuantities) {
        // 生成订单ID
        String orderId = UUID.randomUUID().toString();

        // 创建订单对象
        Order order = new Order(orderId, customerId);
        order.setShippingAddress(shippingAddress);

        // 保存订单
        order = orderRepository.save(order);

        // 添加订单项
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productId = entry.getKey();
            Integer quantity = entry.getValue();

            // 查找商品
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + productId));

            // 检查库存
            if (product.getStock() < quantity) {
                throw new RuntimeException("商品库存不足: " + product.getName());
            }

            // 创建订单项
            OrderItem orderItem = new OrderItem(order, product, quantity);
            order.addItem(orderItem);

            // 减少商品库存
            product.reduceStock(quantity);
            productRepository.save(product);
        }

        // 重新计算订单总金额
        order.recalculateTotalAmount();

        // 保存更新后的订单
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(String orderId) {
        Order order = findOrder(orderId);

        // 检查订单状态是否可以取消
        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("订单已发货或已交付，无法取消");
        }

        // 更新订单状态
        order.setStatus(Order.OrderStatus.CANCELLED);

        // 恢复商品库存
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

        // 检查订单状态
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("只能修改待处理状态的订单");
        }

        // 查找商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在: " + productId));

        // 检查库存
        if (product.getStock() < quantity) {
            throw new RuntimeException("商品库存不足: " + product.getName());
        }

        // 检查订单是否已包含该商品
        OrderItemPK pk = new OrderItemPK(orderId, productId);
        Optional<OrderItem> existingItem = orderItemRepository.findById(pk);

        if (existingItem.isPresent()) {
            // 更新已有订单项的数量
            OrderItem item = existingItem.get();
            // 先恢复原来的库存
            product.setStock(product.getStock() + item.getQuantity());
            // 设置新数量
            item.setQuantity(quantity);
            // 再减去新的库存
            product.reduceStock(quantity);
            productRepository.save(product);
        } else {
            // 创建新的订单项
            OrderItem newItem = new OrderItem(order, product, quantity);
            order.addItem(newItem);
            // 减少商品库存
            product.reduceStock(quantity);
            productRepository.save(product);
        }

        // 重新计算订单总金额
        order.recalculateTotalAmount();

        return orderRepository.save(order);
    }

    @Override
    public Order removeOrderItem(String orderId, String productId) {
        Order order = findOrder(orderId);

        // 检查订单状态
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("只能修改待处理状态的订单");
        }

        // 查找订单项
        OrderItemPK pk = new OrderItemPK(orderId, productId);
        OrderItem item = orderItemRepository.findById(pk)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 恢复商品库存
        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
        productRepository.save(product);

        // 移除订单项
        order.removeItem(item);
        orderItemRepository.delete(item);

        // 重新计算订单总金额
        order.recalculateTotalAmount();

        return orderRepository.save(order);
    }

    @Override
    public Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));
    }

    @Override
    public List<Order> findCustomerOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
