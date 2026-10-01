package com.example.demo.tests;

import org.springframework.context.annotation.Import;
import com.example.demo.TestcontainersConfiguration;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 訂單相關 Repository 的查詢測試（衍生查詢與 JPQL），以及訂單總金額的計算。
 * 每個測試前建立固定的商品與訂單，測試後清除；刪除順序為 訂單項 → 訂單 → 商品，避免外鍵限制。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Order testOrder;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    public void setUp() {
        // 建立測試商品
        testProduct1 = new Product("P001", "測試商品1", new BigDecimal("99.99"), 100);
        testProduct2 = new Product("P002", "測試商品2", new BigDecimal("199.99"), 50);
        testProduct1.setCategory("電子產品");
        testProduct2.setCategory("家居用品");

        productRepository.save(testProduct1);
        productRepository.save(testProduct2);

        // 建立測試訂單
        testOrder = new Order("O001", "C001");
        testOrder.setShippingAddress("測試地址1號");
        testOrder.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(testOrder);

        // 新增訂單項
        OrderItem item1 = new OrderItem(testOrder, testProduct1, 2);
        OrderItem item2 = new OrderItem(testOrder, testProduct2, 1);

        testOrder.addItem(item1);
        testOrder.addItem(item2);

        // 保存訂單和訂單項
        orderRepository.save(testOrder);
    }

    @AfterEach
    public void tearDown() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    public void testFindByCustomerId() {
        List<Order> orders = orderRepository.findByCustomerId("C001");
        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals("O001", orders.get(0).getId());
    }

    @Test
    public void testFindByStatus() {
        List<Order> orders = orderRepository.findByStatus(Order.OrderStatus.PENDING);
        assertNotNull(orders);
        assertTrue(orders.size() > 0);
        assertEquals(Order.OrderStatus.PENDING, orders.get(0).getStatus());
    }

    @Test
    public void testFindOrdersContainingProduct() {
        List<Order> orders = orderRepository.findOrdersContainingProduct("P001");
        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals("O001", orders.get(0).getId());
    }

    @Test
    public void testOrderTotalAmount() {
        Order order = orderRepository.findById("O001").orElse(null);
        assertNotNull(order);

        // 驗證總金額計算: 2 * 99.99 + 1 * 199.99 = 399.97
        assertEquals(0, new BigDecimal("399.97").compareTo(order.getTotalAmount()));
    }
}
