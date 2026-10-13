package com.example.demo.order;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.KafkaException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <pre>
 * POST /api/orders              建立訂單（PENDING）並送出事件 → 202
 * GET  /api/orders/{orderId}    查詢訂單：Shipping 服務回覆後才會變成 SHIPPED / REJECTED
 * </pre>
 */
@RestController
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	public record PlaceOrder(String customer, String item, int quantity) {
	}

	// 202 Accepted：訂單建立了，但出貨的結果之後才知道
	@PostMapping("/api/orders")
	public ResponseEntity<Order> place(@RequestBody PlaceOrder request) {
		if (request.customer() == null || request.customer().isBlank() || request.item() == null
				|| request.item().isBlank() || request.quantity() < 1) {
			throw new IllegalArgumentException("customer, item and a positive quantity are required");
		}
		Order order = orderService.place(request.customer(), request.item(), request.quantity());
		return ResponseEntity.accepted().location(URI.create("/api/orders/" + order.id())).body(order);
	}

	@GetMapping("/api/orders/{orderId}")
	public Order find(@PathVariable UUID orderId) {
		return orderService.find(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail badRequest(IllegalArgumentException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(OrderNotFoundException.class)
	public ProblemDetail notFound(OrderNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	// Kafka 送不出去：訂單沒有建立，請稍後再試
	@ExceptionHandler(KafkaException.class)
	public ProblemDetail kafkaUnavailable(KafkaException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "order could not be placed, please try again later");
	}
}
