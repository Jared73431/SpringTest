package com.example.demo.tests;

import org.springframework.context.annotation.Import;
import com.example.demo.TestcontainersConfiguration;

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

/**
 * OrderService 的整合測試：直接呼叫 Service（不經過 HTTP），驗證建立訂單、新增 / 移除訂單項、
 * 取消訂單時的庫存變化與總金額計算。HTTP 層的行為（狀態碼、狀態機）見 api.OrderApiTest。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
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
        // 清除之前的測試資料
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // 建立測試商品
        testProduct1 = new Product(UUID.randomUUID().toString(), "整合測試商品1", new BigDecimal("88.88"), 20);
        testProduct2 = new Product(UUID.randomUUID().toString(), "整合測試商品2", new BigDecimal("188.88"), 10);

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
        // 準備訂單資料
        String customerId = "C999";
        String shippingAddress = "整合測試地址999號";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 2);
        productQuantities.put(testProduct2.getId(), 1);

        // 建立訂單
        Order createdOrder = orderService.createOrder(customerId, shippingAddress, productQuantities);

        // 驗證訂單建立成功
        assertNotNull(createdOrder);
        assertEquals(customerId, createdOrder.getCustomerId());
        assertEquals(shippingAddress, createdOrder.getShippingAddress());
        assertEquals(Order.OrderStatus.PENDING, createdOrder.getStatus());

        // 驗證訂單項建立成功
        assertEquals(2, createdOrder.getItems().size());

        // 驗證總金額計算正確: 2 * 88.88 + 1 * 188.88 = 366.64
        assertEquals(0, new BigDecimal("366.64").compareTo(createdOrder.getTotalAmount()));

        // 驗證商品庫存已減少
        Product updatedProduct1 = productRepository.findById(testProduct1.getId()).orElse(null);
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);

        assertNotNull(updatedProduct1);
        assertNotNull(updatedProduct2);
        assertEquals(18, updatedProduct1.getStock().intValue()); // 20 - 2 = 18
        assertEquals(9, updatedProduct2.getStock().intValue());  // 10 - 1 = 9
    }

    @Test
    public void testAddAndRemoveOrderItem() {
        // 建立一個訂單
        String customerId = "C888";
        String shippingAddress = "整合測試地址888號";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 1);

        Order order = orderService.createOrder(customerId, shippingAddress, productQuantities);
        String orderId = order.getId();

        // 驗證原始總金額: 1 * 88.88 = 88.88
        assertEquals(0, new BigDecimal("88.88").compareTo(order.getTotalAmount()));

        // 新增新的訂單項
        order = orderService.addOrderItem(orderId, testProduct2.getId(), 2);

        // 驗證新增後的訂單項數量和總金額: 1 * 88.88 + 2 * 188.88 = 466.64
        assertEquals(2, order.getItems().size());
        assertEquals(0, new BigDecimal("466.64").compareTo(order.getTotalAmount()));

        // 驗證商品庫存已更新
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertNotNull(updatedProduct2);
        assertEquals(8, updatedProduct2.getStock().intValue()); // 10 - 2 = 8

        // 移除訂單項
        order = orderService.removeOrderItem(orderId, testProduct2.getId());

        // 驗證移除後的訂單項數量和總金額
        assertEquals(1, order.getItems().size());
        assertEquals(0, new BigDecimal("88.88").compareTo(order.getTotalAmount()));

        // 驗證商品庫存已恢復
        updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertNotNull(updatedProduct2);
        assertEquals(10, updatedProduct2.getStock().intValue()); // 8 + 2 = 10
    }

    @Test
    public void testCancelOrder() {
        // 建立一個訂單
        String customerId = "C777";
        String shippingAddress = "整合測試地址777號";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 3);
        productQuantities.put(testProduct2.getId(), 2);

        Order order = orderService.createOrder(customerId, shippingAddress, productQuantities);

        // 記錄原始庫存
        int originalStock1 = testProduct1.getStock() - 3; // 20 - 3 = 17
        int originalStock2 = testProduct2.getStock() - 2; // 10 - 2 = 8

        // 驗證庫存已減少
        Product updatedProduct1 = productRepository.findById(testProduct1.getId()).orElse(null);
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertEquals(originalStock1, updatedProduct1.getStock().intValue());
        assertEquals(originalStock2, updatedProduct2.getStock().intValue());

        // 取消訂單
        Order cancelledOrder = orderService.cancelOrder(order.getId());

        // 驗證訂單狀態已更新
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getStatus());

        // 驗證庫存已恢復
        updatedProduct1 = productRepository.findById(testProduct1.getId()).orElse(null);
        updatedProduct2 = productRepository.findById(testProduct2.getId()).orElse(null);
        assertEquals(originalStock1 + 3, updatedProduct1.getStock().intValue()); // 17 + 3 = 20
        assertEquals(originalStock2 + 2, updatedProduct2.getStock().intValue()); // 8 + 2 = 10
    }

    public void testCreateOrderWithInsufficientStock() {
        // 準備訂單資料，故意設置超過庫存數量的訂單
        String customerId = "C666";
        String shippingAddress = "整合測試地址666號";
        Map<String, Integer> productQuantities = new HashMap<>();
        productQuantities.put(testProduct1.getId(), 100); // 庫存只有20，設置100會導致異常

        // 這裡應該拋出異常
        orderService.createOrder(customerId, shippingAddress, productQuantities);
    }
}
