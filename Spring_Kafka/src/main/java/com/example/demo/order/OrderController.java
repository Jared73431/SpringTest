package com.example.demo.order;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.KafkaConfig;
import com.example.demo.notification.NotificationListener;
import com.example.demo.shipping.ShippingService;

/**
 * 下單只負責「送出事件」，不直接呼叫出貨與通知：送到 Kafka 就回應 202（已受理），之後由各個 consumer 處理。
 *
 * <pre>
 * POST /api/orders                       送出 PLACED（key = orderId）→ 202
 * POST /api/orders/{orderId}/cancel      送出 CANCELLED（同一個 key）→ 202
 * GET  /api/orders/{orderId}/shipment    出貨狀態（consumer 處理後才會有）
 * GET  /api/notifications                送出的通知
 * GET  /api/dead-letters                 送進 Dead Letter Topic 的訊息
 * </pre>
 */
@RestController
public class OrderController {

	private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

	private final ShippingService shippingService;

	private final NotificationListener notifications;

	private final DeadLetterListener deadLetters;

	public OrderController(KafkaTemplate<String, OrderEvent> kafkaTemplate, ShippingService shippingService,
			NotificationListener notifications, DeadLetterListener deadLetters) {
		this.kafkaTemplate = kafkaTemplate;
		this.shippingService = shippingService;
		this.notifications = notifications;
		this.deadLetters = deadLetters;
	}

	public record PlaceOrder(String customer, String item, int quantity) {
	}

	@PostMapping("/api/orders")
	public ResponseEntity<Map<String, String>> place(@RequestBody PlaceOrder request) {
		if (request.customer() == null || request.customer().isBlank() || request.item() == null
				|| request.item().isBlank() || request.quantity() < 1) {
			throw new IllegalArgumentException("customer, item and a positive quantity are required");
		}
		String orderId = UUID.randomUUID().toString();
		// join()：等 Kafka 確認收到（acks=all）才回應，確保事件沒有遺失
		kafkaTemplate.send(KafkaConfig.ORDERS_TOPIC, orderId,
				OrderEvent.placed(orderId, request.customer(), request.item(), request.quantity())).join();
		return ResponseEntity.accepted().location(URI.create("/api/orders/" + orderId + "/shipment"))
				.body(Map.of("orderId", orderId));
	}

	@PostMapping("/api/orders/{orderId}/cancel")
	public ResponseEntity<Void> cancel(@PathVariable String orderId) {
		kafkaTemplate.send(KafkaConfig.ORDERS_TOPIC, orderId, OrderEvent.cancelled(orderId)).join();
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/api/orders/{orderId}/shipment")
	public ResponseEntity<Map<String, String>> shipment(@PathVariable String orderId) {
		return ResponseEntity.of(shippingService.status(orderId).map(status -> Map.of("status", status.name())));
	}

	@GetMapping("/api/notifications")
	public List<String> notifications() {
		return notifications.sent();
	}

	@GetMapping("/api/dead-letters")
	public List<DeadLetterListener.DeadLetter> deadLetters() {
		return deadLetters.received();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail badRequest(IllegalArgumentException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}
}
