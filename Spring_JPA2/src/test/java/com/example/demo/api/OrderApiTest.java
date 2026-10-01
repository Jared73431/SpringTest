package com.example.demo.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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
@AutoConfigureTestRestTemplate
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

	private ResponseEntity<String> createOrderForResponse(String productId, int quantity) {
		CreateOrderRequest request = new CreateOrderRequest();
		request.setCustomerId("C1");
		request.setShippingAddress("Taipei");
		request.setProductQuantities(Map.of(productId, quantity));
		return restTemplate.postForEntity("/api/orders", request, String.class);
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
	void createOrder_shouldReturnConflict_whenStockIsInsufficient() {
		String productId = createProduct(1);

		ResponseEntity<String> response = createOrderForResponse(productId, 5);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(stockOf(productId)).isEqualTo(1);
		assertThat(orderRepository.count()).isZero();
	}

	// 請求內容參照的商品不存在：屬於請求錯誤（400），不是 URL 資源不存在（404）
	@Test
	void createOrder_shouldReturnBadRequest_whenProductNotExists() {
		ResponseEntity<String> response = createOrderForResponse("not-exists", 1);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).contains("not-exists");
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
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).contains("not-exists");
	}

	@Test
	void cancelOrder_shouldReturnNotFound_whenOrderNotExists() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/orders/not-exists/cancel", null,
				String.class);

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

	@Test
	void cancelOrder_shouldRestoreStock_whenOrderIsProcessing() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 2).getBody().getId();
		updateStatus(orderId, "PROCESSING");

		ResponseEntity<OrderDTO> response = restTemplate.postForEntity("/api/orders/{id}/cancel", null,
				OrderDTO.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(stockOf(productId)).isEqualTo(10);
	}

	// 修正前：已取消的訂單可以再取消，每次都會補回庫存
	@Test
	void cancelOrder_shouldReturnConflictAndKeepStock_whenOrderAlreadyCancelled() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 2).getBody().getId();
		restTemplate.postForEntity("/api/orders/{id}/cancel", null, OrderDTO.class, orderId);

		ResponseEntity<String> response = restTemplate.postForEntity("/api/orders/{id}/cancel", null,
				String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(stockOf(productId)).isEqualTo(10);
	}

	@Test
	void cancelOrder_shouldReturnConflict_whenOrderShipped() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 2).getBody().getId();
		updateStatus(orderId, "PROCESSING");
		updateStatus(orderId, "SHIPPED");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/orders/{id}/cancel", null,
				String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(stockOf(productId)).isEqualTo(8);
	}

	// 修正前：數量為負數也能下單，庫存反而增加
	@ParameterizedTest
	@ValueSource(ints = { 0, -5 })
	void createOrder_shouldReturnBadRequest_whenQuantityIsNotPositive(int quantity) {
		String productId = createProduct(10);

		ResponseEntity<String> response = createOrderForResponse(productId, quantity);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(stockOf(productId)).isEqualTo(10);
		assertThat(orderRepository.count()).isZero();
	}

	@Test
	void updateOrderStatus_shouldMoveForward_whenTransitionAllowed() {
		String orderId = createOrder(createProduct(10), 1).getBody().getId();

		assertThat(updateStatus(orderId, "PROCESSING").getBody().getStatus()).isEqualTo("PROCESSING");
		assertThat(updateStatus(orderId, "SHIPPED").getBody().getStatus()).isEqualTo("SHIPPED");
		assertThat(updateStatus(orderId, "DELIVERED").getBody().getStatus()).isEqualTo("DELIVERED");
	}

	// 修正前：訂單狀態沒有轉換規則，已送達的訂單可以改回 PENDING
	@Test
	void updateOrderStatus_shouldReturnConflict_whenOrderDelivered() {
		String orderId = createOrder(createProduct(10), 1).getBody().getId();
		updateStatus(orderId, "PROCESSING");
		updateStatus(orderId, "SHIPPED");
		updateStatus(orderId, "DELIVERED");

		ResponseEntity<String> response = restTemplate.exchange("/api/orders/{id}/status?status=PENDING",
				HttpMethod.PUT, null, String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void updateOrderStatus_shouldReturnConflict_whenSkippingStatus() {
		String orderId = createOrder(createProduct(10), 1).getBody().getId();

		ResponseEntity<String> response = restTemplate.exchange("/api/orders/{id}/status?status=SHIPPED",
				HttpMethod.PUT, null, String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	// 取消必須透過 /cancel，才會補回庫存
	@Test
	void updateOrderStatus_shouldReturnConflictAndKeepStock_whenSetToCancelledDirectly() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 2).getBody().getId();

		ResponseEntity<String> response = restTemplate.exchange("/api/orders/{id}/status?status=CANCELLED",
				HttpMethod.PUT, null, String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).contains("cancel");
		assertThat(stockOf(productId)).isEqualTo(8);
	}

	@Test
	void addOrderItem_shouldReturnConflict_whenOrderNotPending() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 1).getBody().getId();
		updateStatus(orderId, "PROCESSING");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/orders/{id}/items",
				Map.of("productId", productId, "quantity", 1), String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void addOrderItem_shouldReturnBadRequestAndKeepStock_whenQuantityIsNotPositive() {
		String productId = createProduct(10);
		String orderId = createOrder(productId, 1).getBody().getId();

		ResponseEntity<String> response = restTemplate.postForEntity("/api/orders/{id}/items",
				Map.of("productId", productId, "quantity", -3), String.class, orderId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(stockOf(productId)).isEqualTo(9);
	}

	// 修正前：同時下單時「讀取 → 扣庫存 → 寫回」會互相覆蓋（lost update），庫存比實際售出多
	// 修正後：Product 使用 @Version 樂觀鎖，衝突的請求回 409，庫存永遠等於「初始庫存 − 成功售出數量」
	@Test
	void createOrder_shouldNeverLoseStockUpdates_whenOrdersAreConcurrent() throws Exception {
		String productId = createProduct(100);
		int requests = 40;
		ExecutorService executor = Executors.newFixedThreadPool(requests);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<HttpStatusCode>> results = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			results.add(executor.submit(() -> {
				start.await();
				return createOrderForResponse(productId, 1).getStatusCode();
			}));
		}
		start.countDown();
		int created = 0;
		for (Future<HttpStatusCode> result : results) {
			HttpStatusCode status = result.get(30, TimeUnit.SECONDS);
			assertThat(status).isIn(HttpStatus.CREATED, HttpStatus.CONFLICT);
			if (status.value() == HttpStatus.CREATED.value()) {
				created++;
			}
		}
		executor.shutdown();

		assertThat(created).isPositive();
		assertThat(stockOf(productId)).isEqualTo(100 - created);
		assertThat(orderItemRepository.count()).isEqualTo(created);
	}

	@Test
	void removeOrderItem_shouldReturnNotFound_whenItemNotInOrder() {
		String orderId = createOrder(createProduct(10), 1).getBody().getId();

		ResponseEntity<String> response = restTemplate.exchange("/api/orders/{id}/items/{pid}", HttpMethod.DELETE,
				null, String.class, orderId, "not-in-order");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
	void createProduct_shouldReturnConflict_whenIdAlreadyExists() {
		String productId = createProduct(1);

		ResponseEntity<String> response = restTemplate.postForEntity("/api/products",
				Map.of("id", productId, "name", "Pen", "price", 10, "stock", 5), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void updateProduct_shouldReturnNotFound_whenProductNotExists() {
		ResponseEntity<String> response = restTemplate.exchange("/api/products/not-exists", HttpMethod.PUT,
				new HttpEntity<>(Map.of("name", "x", "price", 1, "stock", 1)),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
