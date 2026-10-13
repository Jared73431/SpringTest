package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import com.example.demo.KafkaContainerTestBase;
import com.example.demo.notification.NotificationListener;
import com.example.demo.shipping.ShippingService;

/**
 * 第 1 課：Producer 送出事件、Consumer 收到事件。
 *
 * <pre>
 * POST /api/orders → KafkaTemplate.send("orders", orderId, OrderEvent) → 回應 202（已受理）
 *                                     │
 *               Kafka 的 topic「orders」（訊息存在磁碟，consumer 讀完也不會刪除）
 *                                     │
 *              @KafkaListener（出貨、通知）各自讀取 → 處理
 * </pre>
 *
 * 和 REST 呼叫的差別：下單的一方不等出貨處理完，也不需要知道有誰在處理；出貨服務暫時停掉，事件也不會遺失，恢復後接著讀。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L01_ProducerConsumerTest extends KafkaContainerTestBase {

	@LocalServerPort
	private int port;

	@Autowired
	private ShippingService shippingService;

	@Autowired
	private NotificationListener notifications;

	private String placeOrder(String customer, String item, int quantity) {
		var response = RestClient.create("http://localhost:" + port).post().uri("/api/orders")
				.body(Map.of("customer", customer, "item", item, "quantity", quantity))
				.retrieve().toEntity(new ParameterizedTypeReference<Map<String, String>>() {
				});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		return response.getBody().get("orderId");
	}

	@Test
	void placedOrder_shouldReachTheConsumers() {
		String orderId = placeOrder("alice", "book", 2);

		// consumer 是非同步處理的：回應 202 時還不一定處理完，所以要等
		await().atMost(Duration.ofSeconds(10))
				.until(() -> shippingService.status(orderId).equals(Optional.of(ShippingService.Status.WAITING_FOR_SHIPMENT)));
		await().atMost(Duration.ofSeconds(10)).until(() -> notifications.sent()
				.contains("alice, your order " + orderId + " has been placed"));
	}

	// 送進 Kafka 的是 key（orderId）與 JSON；Spring 另外在標頭記下類別名稱，consumer 據此轉回物件
	@Test
	void messageInTheTopic_shouldBeJsonWithTheOrderIdAsKey() {
		String orderId = placeOrder("bob", "pen", 1);

		try (var consumer = new KafkaConsumer<String, String>(Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
				ConsumerConfig.GROUP_ID_CONFIG, "lesson-1-reader",
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
				new StringDeserializer(), new StringDeserializer())) {
			consumer.subscribe(List.of("orders"));
			var record = await().atMost(Duration.ofSeconds(10)).until(() -> {
				for (var r : consumer.poll(Duration.ofMillis(500))) {
					if (orderId.equals(r.key())) {
						return r;
					}
				}
				return null;
			}, r -> r != null);

			assertThat(record.value()).contains("\"orderId\":\"" + orderId + "\"").contains("\"type\":\"PLACED\"")
					.contains("\"customer\":\"bob\"");
			assertThat(new String(record.headers().lastHeader("__TypeId__").value()))
					.isEqualTo("com.example.demo.order.OrderEvent");
		}
	}

	@Test
	void invalidOrder_shouldBeRejectedBeforeReachingKafka() {
		var response = RestClient.builder().baseUrl("http://localhost:" + port)
				.defaultStatusHandler(status -> true, (request, resp) -> {
				})
				.build().post().uri("/api/orders").body(Map.of("customer", "alice", "item", "book", "quantity", 0))
				.retrieve().toBodilessEntity();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
