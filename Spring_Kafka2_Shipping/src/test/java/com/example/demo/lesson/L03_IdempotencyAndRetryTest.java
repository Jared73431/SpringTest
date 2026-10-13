package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.demo.ContainersTestBase;
import com.example.demo.config.KafkaConfig;
import com.example.demo.event.ShipmentEvent.Status;
import com.example.demo.shipment.ShipmentRepository;
import com.example.demo.shipment.ShippingService;

/**
 * 第 3 課：重複送達與重試 —— 「處理過的紀錄」要和業務資料在同一個交易。
 *
 * <pre>
 * 交易開始
 *   INSERT processed_event（已經有 → 重複的事件，略過）
 *   INSERT shipment
 *   送出回覆（失敗 → 拋出例外）
 * 交易結束（失敗 → 全部 rollback，連 processed_event 也沒有 → 重試時從頭再處理一次）
 * </pre>
 *
 * 如果 processed_event 不在同一個交易：回覆送不出去時「處理過」已經記下了，重試時會被當成重複而略過，
 * 這筆訂單就永遠沒有回覆。
 */
@SpringBootTest
class L03_IdempotencyAndRetryTest extends ContainersTestBase {

	@MockitoSpyBean
	private ShippingService shippingService;

	@MockitoSpyBean
	private ShipmentRepository shipmentRepository;

	@MockitoSpyBean
	private KafkaTemplate<?, ?> kafkaTemplate;

	@Autowired
	private JdbcClient jdbcClient;

	private long shipmentCount(UUID orderId) {
		return jdbcClient.sql("SELECT count(*) FROM shipment WHERE order_id = :orderId").param("orderId", orderId)
				.query(Long.class).single();
	}

	@SuppressWarnings("unchecked")
	@Test
	void duplicateEvent_shouldBeShippedAndRepliedOnlyOnce() {
		UUID orderId = UUID.randomUUID();
		String event = L01_ShipAndReplyTest.orderPlaced(UUID.randomUUID(), orderId, 1);

		sendJson(KafkaConfig.ORDER_EVENTS, orderId.toString(), event);
		sendJson(KafkaConfig.ORDER_EVENTS, orderId.toString(), event); // 模擬重複送達

		// 兩則都收到了，但只出貨、回覆一次
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(shippingService, times(2))
				.handle(argThat(e -> e != null && orderId.equals(e.orderId()))));
		verify(shipmentRepository, times(1)).insert(eq(orderId), any(), any());
		verify((KafkaTemplate<String, Object>) kafkaTemplate, times(1)).send(eq(KafkaConfig.SHIPMENT_EVENTS), eq(orderId.toString()), any());
	}

	@SuppressWarnings("unchecked")
	@Test
	void replyFailure_shouldRollBackAndBeRetried() {
		UUID orderId = UUID.randomUUID();
		var attempts = new AtomicInteger();
		// 第一次送出回覆失敗（例如 Kafka 暫時連不上），之後正常
		doAnswer(invocation -> {
			if (attempts.incrementAndGet() == 1) {
				throw new KafkaException("broker unavailable");
			}
			return invocation.callRealMethod();
		}).when((KafkaTemplate<String, Object>) kafkaTemplate)
				.send(eq(KafkaConfig.SHIPMENT_EVENTS), eq(orderId.toString()), any());

		sendJson(KafkaConfig.ORDER_EVENTS, orderId.toString(), L01_ShipAndReplyTest.orderPlaced(UUID.randomUUID(), orderId, 1));

		// 重試後成功：出貨寫了兩次（第一次 rollback），資料庫只有一筆，回覆送出一則
		var replies = readRecords(KafkaConfig.SHIPMENT_EVENTS, orderId.toString());
		assertThat(replies).hasSize(1);
		assertThat(attempts).hasValue(2);
		verify(shipmentRepository, times(2)).insert(eq(orderId), any(), any());
		assertThat(shipmentCount(orderId)).isEqualTo(1);
		assertThat(shippingService.find(orderId)).hasValueSatisfying(s -> assertThat(s.status()).isEqualTo(Status.SHIPPED));
	}
}
