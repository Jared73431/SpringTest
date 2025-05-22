package com.example.demo.tests;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.entity.Order;
import com.example.demo.entity.Product;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.OrderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    public void setUp() {
        // 清除之前的测试数据
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // 创建测试商品
        testProduct1 = new Product(UUID.randomUUID().toString(), "集成测试商品1", new BigDecimal("88.88"), 20);
        testProduct2 = new Product(UUID.randomUUID().toString(), "集成测试商品2", new BigDecimal("188.88"), 10);

        productRepository.save(testProduct1);
        productRepository.save(testProduct2);
    }

    @AfterEach
    public void tearDown() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    public void testCreateOrder() {
        // 准备订单数据
        String customerId = "C999";
        String shippingAddress = "集成测试地址999号";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 2);
        productQuantities.put(testProduct2.getId(), 1);

        // 创建订单
        Order createdOrder = orderService.createOrder(customerId, shippingAddress, productQuantities);

        // 验证订单创建成功
        assertNotNull(createdOrder);
        assertEquals(customerId, createdOrder.getCustomerId());
        assertEquals(shippingAddress, createdOrder.getShippingAddress());
        assertEquals(Order.OrderStatus.PENDING, createdOrder.getStatus());

        // 验证订单项创建成功
        assertEquals(2, createdOrder.getItems().size());

        // 验证总金额计算正确: 2 * 88.88 + 1 * 188.88 = 366.64
        assertEquals(0, new BigDecimal("366.64").compareTo(createdOrder.getTotalAmount()));

        // 验证商品库存已减少
        Product updatedProduct1 = productRepository.findById(testProduct1.getId()).orElse(null);
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);

        assertNotNull(updatedProduct1);
        assertNotNull(updatedProduct2);
        assertEquals(18, updatedProduct1.getStock().intValue()); // 20 - 2 = 18
        assertEquals(9, updatedProduct2.getStock().intValue());  // 10 - 1 = 9
    }

    @Test
    public void testAddAndRemoveOrderItem() {
        // 创建一个订单
        String customerId = "C888";
        String shippingAddress = "集成测试地址888号";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 1);

        Order order = orderService.createOrder(customerId, shippingAddress, productQuantities);
        String orderId = order.getId();

        // 验证原始总金额: 1 * 88.88 = 88.88
        assertEquals(0, new BigDecimal("88.88").compareTo(order.getTotalAmount()));

        // 添加新的订单项
        order = orderService.addOrderItem(orderId, testProduct2.getId(), 2);

        // 验证添加后的订单项数量和总金额: 1 * 88.88 + 2 * 188.88 = 466.64
        assertEquals(2, order.getItems().size());
        assertEquals(0, new BigDecimal("466.64").compareTo(order.getTotalAmount()));

        // 验证商品库存已更新
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertNotNull(updatedProduct2);
        assertEquals(8, updatedProduct2.getStock().intValue()); // 10 - 2 = 8

        // 移除订单项
        order = orderService.removeOrderItem(orderId, testProduct2.getId());

        // 验证移除后的订单项数量和总金额
        assertEquals(1, order.getItems().size());
        assertEquals(0, new BigDecimal("88.88").compareTo(order.getTotalAmount()));

        // 验证商品库存已恢复
        updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertNotNull(updatedProduct2);
        assertEquals(10, updatedProduct2.getStock().intValue()); // 8 + 2 = 10
    }

    @Test
    public void testCancelOrder() {
        // 创建一个订单
        String customerId = "C777";
        String shippingAddress = "集成测试地址777号";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 3);
        productQuantities.put(testProduct2.getId(), 2);

        Order order = orderService.createOrder(customerId, shippingAddress, productQuantities);

        // 记录原始库存
        int originalStock1 = testProduct1.getStock() - 3; // 20 - 3 = 17
        int originalStock2 = testProduct2.getStock() - 2; // 10 - 2 = 8

        // 验证库存已减少
        Product updatedProduct1 = productRepository.findById(testProduct1.getId()).orElse(null);
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertEquals(originalStock1, updatedProduct1.getStock().intValue());
        assertEquals(originalStock2, updatedProduct2.getStock().intValue());

        // 取消订单
        Order cancelledOrder = orderService.cancelOrder(order.getId());

        // 验证订单状态已更新
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getStatus());

        // 验证库存已恢复
        updatedProduct1 = productRepository.findById(testProduct1.getId()).orElse(null);
        updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertEquals(originalStock1 + 3, updatedProduct1.getStock().intValue()); // 17 + 3 = 20
        assertEquals(originalStock2 + 2, updatedProduct2.getStock().intValue()); // 8 + 2 = 10
    }

    public void testCreateOrderWithInsufficientStock() {
        // 准备订单数据，故意设置超过库存数量的订单
        String customerId = "C666";
        String shippingAddress = "集成测试地址666号";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 100); // 库存只有20，设置100会导致异常

        // 这里应该抛出异常
        orderService.createOrder(customerId, shippingAddress, productQuantities);
    }
}
