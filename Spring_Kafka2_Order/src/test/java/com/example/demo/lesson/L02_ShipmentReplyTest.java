package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.KafkaHeaders;

import com.example.demo.ContainersTestBase;
import com.example.demo.config.KafkaConfig;
import com.example.demo.order.Order;
import com.example.demo.order.OrderService;

/**
 * 第 2 課：Shipping 服務的回覆也是事件；最終一致性。
 *
 * <pre>
 * Order 服務                           Shipping 服務
 *   訂單 PENDING ── order-events ──→   出貨 / 拒絕
 *   訂單 SHIPPED ←─ shipment-events ──  （回覆）
 * </pre>
 *
 * 從送出事件到收到回覆之間，訂單是 PENDING：兩個服務的資料暫時不一致，之後才一致。
 * 呼叫端（前端）要能處理這個中間狀態，例如顯示「處理中」、稍後再查詢。
 */
@SpringBootTest
class L02_ShipmentReplyTest extends ContainersTestBase {

	@Autowired
	private OrderService orderService;

	private static String shipmentEvent(UUID orderId, String status, String reason) {
		return """
				{"eventId":"%s","orderId":"%s","status":"%s","reason":%s,"occurredAt":"2026-10-13T08:00:00Z"}
				""".formatted(UUID.randomUUID(), orderId, status, reason == null ? "null" : "\"" + reason + "\"");
	}

	private Order.Status statusOf(UUID orderId) {
		return orderService.find(orderId).orElseThrow().status();
	}

	@Test
	void order_shouldBecomeShipped_whenTheShipmentEventArrives() {
		Order order = orderService.place("bob", "pen", 1);
		assertThat(order.status()).isEqualTo(Order.Status.PENDING);

		sendJson(KafkaConfig.SHIPMENT_EVENTS, order.id().toString(), shipmentEvent(order.id(), "SHIPPED", null));

		await().atMost(Duration.ofSeconds(10)).until(() -> statusOf(order.id()) == Order.Status.SHIPPED);
	}

	// 業務上的「拒絕」也是一種正常的回覆，不是錯誤：不會進 Dead Letter Topic
	@Test
	void order_shouldBecomeRejectedWithTheReason_whenTheShipmentIsRejected() {
		Order order = orderService.place("carol", "sofa", 20);

		sendJson(KafkaConfig.SHIPMENT_EVENTS, order.id().toString(),
				shipmentEvent(order.id(), "REJECTED", "quantity 20 exceeds 10"));

		await().atMost(Duration.ofSeconds(10)).until(() -> statusOf(order.id()) == Order.Status.REJECTED);
		assertThat(orderService.find(order.id()).orElseThrow().reason()).isEqualTo("quantity 20 exceeds 10");
	}

	// 回覆了一筆不存在的訂單：重試也沒用，直接送到 shipment-events-dlt，由人查看
	@Test
	void replyForAnUnknownOrder_shouldGoToTheDeadLetterTopic() {
		UUID unknown = UUID.randomUUID();

		sendJson(KafkaConfig.SHIPMENT_EVENTS, unknown.toString(), shipmentEvent(unknown, "SHIPPED", null));

		var deadLetters = readRecords(KafkaConfig.SHIPMENT_EVENTS_DLT, unknown.toString());
		assertThat(deadLetters).hasSize(1);
		var reason = deadLetters.getFirst().headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
		assertThat(new String(reason.value(), StandardCharsets.UTF_8)).contains("not found");
	}
}
