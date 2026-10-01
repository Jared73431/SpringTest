package com.example.demo.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.request.CreateOrderRequest;

/**
 * 訂單與商品 API 的行為測試（透過真實 HTTP 呼叫）。
 * 標示 [Potential Bug] 的測試記錄的是「目前的行為」，修正時會一併修改。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class OrderApiTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@BeforeEach
	void cleanDatabase() {
		orderItemRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
	}

	private String createProduct(int stock) {
		Product product = new Product(UUID.randomUUID().toString(), "Book", new BigDecimal("100.00"), stock);
		return productRepository.save(product).getId();
	}

	private int stockOf(String productId) {
		return productRepository.findById(productId).orElseThrow().getStock();
	}

	private ResponseEntity<OrderDTO> createOrder(String productId, int quantity) {
		CreateOrderRequest request = new CreateOrderRequest();
		request.setCustomerId("C1");
		request.setShippingAddress("Taipei");
		request.setProductQuantities(Map.of(productId, quantity));
		return restTemplate.postForEntity("/api/orders", request, OrderDTO.class);
	}

	private ResponseEntity<OrderDTO> updateStatus(String orderId, String status) {
		return restTemplate.exchange("/api/orders/{id}/status?status={status}", HttpMethod.PUT, null,
				OrderDTO.class, orderId, status);
	}

	// ===== 訂單 =====

	@Test
	void createOrder_shouldReturnCreatedAndReduceStock_whenStockIsEnough() {
		String productId = createProduct(10);

		ResponseEntity<OrderDTO> response = createOrder(productId, 2);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasPath("/api/orders/" + response.getBody().getId());
		assertThat(response.getBody().getStatus()).isEqualTo("PENDING");
		assertThat(response.getBody().getItems()).hasSize(1);
		assertThat(response.getBody().getTotalAmount()).isEqualByComparingTo("200.00");
		assertThat(stockOf(productId)).isEqualTo(8);
	}

	@Test
	void createOrder_shouldReturnBadRequest_whenStockIsInsufficient() {
		String productId = createProduct(1);

		ResponseEntity<OrderDTO> response = createOrder(productId, 5);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(stockOf(productId)).isEqualTo(1);
		assertThat(orderRepository.count()).isZero();
	}

	@Test
	void createOrder_shouldReturnBadRequest_whenProductNotExists() {
		ResponseEntity<OrderDTO> response = createOrder("not-exists", 1);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(orderRepository.count()).isZero();
	}

	@Test
	void getOrderById_shouldReturnOrder_whenOrderExists() {
		String orderId = createOrder(createProduct(10), 1).getBody().getId();

		ResponseEntity<OrderDTO> response = restTemplate.getForEntity("/api/orders/{id}", OrderDTO.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getItems()).hasSize(1);
	}

	@Test
	void getOrderById_shouldReturnNotFound_whenOrderNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/orders/not-exists", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void cancelOrder_shouldRestoreStock_whenOrderIsPending() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 2).getBody().getId();

		ResponseEntity<OrderDTO> response = restTemplate.postForEntity("/api/orders/{id}/cancel", null,
				OrderDTO.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStatus()).isEqualTo("CANCELLED");
		assertThat(stockOf(productId)).isEqualTo(10);
	}

	// [Potential Bug] 已取消的訂單可以再取消，每次都會補回庫存
	@Test
	void cancelOrder_shouldRestoreStockAgain_whenOrderAlreadyCancelled() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 2).getBody().getId();
		restTemplate.postForEntity("/api/orders/{id}/cancel", null, OrderDTO.class, orderId);

		ResponseEntity<OrderDTO> response = restTemplate.postForEntity("/api/orders/{id}/cancel", null,
				OrderDTO.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(stockOf(productId)).isEqualTo(12);
	}

	@Test
	void cancelOrder_shouldReturnBadRequest_whenOrderShipped() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 2).getBody().getId();
		updateStatus(orderId, "SHIPPED");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/orders/{id}/cancel", null,
				String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(stockOf(productId)).isEqualTo(8);
	}

	// [Potential Bug] 數量為負數也能下單，庫存反而增加
	@Test
	void createOrder_shouldIncreaseStock_whenQuantityIsNegative() {
		String productId = createProduct(10);

		ResponseEntity<OrderDTO> response = createOrder(productId, -5);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(stockOf(productId)).isEqualTo(15);
	}

	// [Potential Bug] 訂單狀態沒有轉換規則，已送達的訂單可以改回 PENDING
	@Test
	void updateOrderStatus_shouldAllowAnyTransition_whenOrderDelivered() {
		String orderId = createOrder(createProduct(10), 1).getBody().getId();
		updateStatus(orderId, "DELIVERED");

		ResponseEntity<OrderDTO> response = updateStatus(orderId, "PENDING");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStatus()).isEqualTo("PENDING");
	}

	@Test
	void addOrderItem_shouldReturnBadRequest_whenOrderNotPending() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 1).getBody().getId();
		updateStatus(orderId, "SHIPPED");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/orders/{id}/items",
				Map.of("productId", productId, "quantity", 1), String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	// ===== 商品 =====

	@Test
	void createProduct_shouldGenerateIdAndReturnCreated_whenIdNotGiven() {
		ResponseEntity<ProductDTO> response = restTemplate.postForEntity("/api/products",
				Map.of("name", "Pen", "price", 10, "stock", 5), ProductDTO.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody().getId()).isNotBlank();
		assertThat(response.getHeaders().getLocation()).hasPath("/api/products/" + response.getBody().getId());
	}

	// [Potential Bug] 沒有輸入驗證，缺少必填欄位時由資料庫限制擋下，回 500
	@Test
	void createProduct_shouldReturnInternalServerError_whenRequiredFieldMissing() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/products", Map.of(), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void updateProduct_shouldReturnNotFound_whenProductNotExists() {
		ResponseEntity<String> response = restTemplate.exchange("/api/products/not-exists", HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", "x", "price", 1, "stock", 1)),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
