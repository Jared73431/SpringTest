package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.demo.KafkaContainerTestBase;
import com.example.demo.order.DeadLetterListener;
import com.example.demo.order.DeadLetterListener.DeadLetter;
import com.example.demo.order.OrderEvent;
import com.example.demo.shipping.ShippingService;

/**
 * 第 5 課：處理失敗怎麼辦？重試、Dead Letter Topic。
 *
 * <pre>
 * 處理時拋出例外
 *   ├─ 暫時性的錯誤 → 每 500ms 重試，最多 2 次 → 成功就繼續
 *   ├─ 重試後還是失敗 → 送到 orders-dlt，繼續處理下一則
 *   ├─ 業務規則不允許（ShippingRejectedException）→ 不重試，直接送到 orders-dlt
 *   └─ JSON 壞掉（poison pill）→ 根本轉不成物件，直接送到 orders-dlt
 * </pre>
 *
 * 為什麼要有 DLT：如果一直重試同一則失敗的訊息，同一個 partition 後面的訊息全部卡住。
 * 把它移到 DLT，後面的訊息照常處理；DLT 的訊息由人處理後再重新送出。
 */
@SpringBootTest
class L05_ErrorHandlingTest extends KafkaContainerTestBase {

	@MockitoSpyBean
	private ShippingService shippingService;

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaTemplate;

	@Autowired
	private DeadLetterListener deadLetters;

	private DeadLetter awaitDeadLetter(String key) {
		return await().atMost(Duration.ofSeconds(15))
				.until(() -> deadLetters.received().stream().filter(d -> key.equals(d.key())).findFirst().orElse(null),
						d -> d != null);
	}

	@Test
	void transientError_shouldBeRetriedAndThenSucceed() {
		String orderId = UUID.randomUUID().toString();
		var attempts = new AtomicInteger();
		// 前兩次失敗（例如資料庫暫時斷線），第三次成功
		doAnswer(invocation -> {
			if (attempts.incrementAndGet() <= 2) {
				throw new IllegalStateException("database unavailable");
			}
			return invocation.callRealMethod();
		}).when(shippingService).handle(argThat(event -> event != null && orderId.equals(event.orderId())));

		kafkaTemplate.send("orders", orderId, OrderEvent.placed(orderId, "frank", "chair", 1));

		await().atMost(Duration.ofSeconds(10)).until(() -> shippingService.status(orderId).isPresent());
		assertThat(attempts).hasValue(3);
		assertThat(deadLetters.received()).noneMatch(d -> orderId.equals(d.key()));
	}

	@Test
	void persistentError_shouldGoToTheDeadLetterTopicAfterTheRetries() {
		String orderId = UUID.randomUUID().toString();
		doThrow(new IllegalStateException("database unavailable")).when(shippingService)
				.handle(argThat(event -> event != null && orderId.equals(event.orderId())));

		kafkaTemplate.send("orders", orderId, OrderEvent.placed(orderId, "grace", "sofa", 1));

		assertThat(awaitDeadLetter(orderId).reason()).contains("database unavailable");
		verify(shippingService, atLeast(3)).handle(argThat(event -> event != null && orderId.equals(event.orderId())));
	}

	// 一次超過 10 件：業務規則不允許，重試也沒用 → 不重試，直接送到 DLT
	@Test
	void rejectedOrder_shouldGoToTheDeadLetterTopicWithoutRetrying() {
		String orderId = UUID.randomUUID().toString();

		kafkaTemplate.send("orders", orderId, OrderEvent.placed(orderId, "henry", "book", 11));

		assertThat(awaitDeadLetter(orderId).reason()).contains("quantity 11 exceeds 10");
		verify(shippingService, atLeast(1)).handle(argThat(event -> event != null && orderId.equals(event.orderId())));
		assertThat(shippingService.status(orderId)).isEmpty();
	}

	// 格式錯誤的訊息：轉不成 OrderEvent，直接送到 DLT；同一個 partition 後面的訊息照常處理
	@Test
	void poisonPill_shouldGoToTheDeadLetterTopic_andNotBlockTheFollowingMessages() {
		String orderId = UUID.randomUUID().toString();
		try (var producer = new KafkaProducer<String, byte[]>(
				Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()),
				new StringSerializer(), new ByteArraySerializer())) {
			producer.send(new ProducerRecord<>("orders", orderId, "this is not json".getBytes(StandardCharsets.UTF_8)));
		}
		kafkaTemplate.send("orders", orderId, OrderEvent.placed(orderId, "ivy", "pen", 1)); // 同一個 key、同一個 partition

		assertThat(awaitDeadLetter(orderId).value()).isEqualTo("this is not json");
		await().atMost(Duration.ofSeconds(10)).until(() -> shippingService.status(orderId).isPresent());
	}
}
