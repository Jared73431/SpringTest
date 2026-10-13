package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.demo.KafkaContainerTestBase;
import com.example.demo.order.OrderEvent;
import com.example.demo.shipping.ShippingListener;
import com.example.demo.shipping.ShippingService;

/**
 * 第 4 課：Offset 與「至少送達一次」。
 *
 * <pre>
 * partition 0：[0][1][2][3][4][5]
 *                          ↑ group「shipping」commit 的 offset = 4：下一則從 4 開始讀
 * </pre>
 *
 * <ul>
 * <li>每個 consumer group 在每個 partition 各記一個 offset（讀到哪裡），存在 Kafka 裡</li>
 * <li>處理完才 commit：處理到一半當機，重新啟動後從上次 commit 的位置再讀一次 → 訊息<b>不會遺失</b>，但<b>可能重複</b></li>
 * <li>所以 consumer 要能<b>冪等</b>處理：同一個事件處理兩次，結果和一次相同（ShippingService 用 eventId 記住處理過的事件）</li>
 * </ul>
 */
@SpringBootTest
class L04_OffsetAndIdempotencyTest extends KafkaContainerTestBase {

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaTemplate;

	@Autowired
	private ShippingService shippingService;

	private Map<TopicPartition, OffsetAndMetadata> committedOffsets(String group) throws Exception {
		try (var admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
			return admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get();
		}
	}

	// 處理完之後，group 在那個 partition 的 offset 前進到「下一則」的位置
	@Test
	void offset_shouldBeCommittedAfterTheEventIsHandled() {
		String orderId = UUID.randomUUID().toString();

		var sent = kafkaTemplate.send("orders", orderId, OrderEvent.placed(orderId, "dave", "lamp", 1)).join()
				.getRecordMetadata();

		var partition = new TopicPartition("orders", sent.partition());
		await().atMost(Duration.ofSeconds(10)).until(() -> {
			var committed = committedOffsets(ShippingListener.GROUP).get(partition);
			return committed != null && committed.offset() > sent.offset();
		});
	}

	// 同一個事件（同一個 eventId）送達兩次：只處理一次
	@Test
	void duplicateEvent_shouldBeHandledOnlyOnce() {
		String orderId = UUID.randomUUID().toString();
		var event = OrderEvent.placed(orderId, "erin", "desk", 1);
		int before = shippingService.handledCount();

		kafkaTemplate.send("orders", orderId, event);
		kafkaTemplate.send("orders", orderId, event); // 模擬重複送達

		await().atMost(Duration.ofSeconds(10)).until(() -> shippingService.status(orderId).isPresent());
		// 再送一則同一個 key 的事件當作「記號」：它被處理時，前面兩則一定都處理完了（同一個 partition 依序處理）
		var marker = OrderEvent.cancelled(orderId);
		kafkaTemplate.send("orders", orderId, marker);
		await().atMost(Duration.ofSeconds(10))
				.until(() -> shippingService.status(orderId).equals(java.util.Optional.of(ShippingService.Status.CANCELLED)));

		assertThat(shippingService.handledCount() - before).isEqualTo(2); // 建立一次 + 取消一次，重複的那次被略過
	}
}
