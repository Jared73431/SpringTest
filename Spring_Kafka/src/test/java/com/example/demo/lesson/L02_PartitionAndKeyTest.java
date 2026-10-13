package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.demo.KafkaContainerTestBase;
import com.example.demo.order.OrderEvent;
import com.example.demo.shipping.ShippingService;

/**
 * 第 2 課：Partition 與 key —— 順序只在同一個 partition 裡保證。
 *
 * <pre>
 * topic「orders」有 3 個 partition：
 *   partition 0：[A 建立][C 建立][A 取消]…
 *   partition 1：[B 建立]…
 *   partition 2：…
 * </pre>
 *
 * <ul>
 * <li>Kafka 依 key 的雜湊值決定 partition：<b>同一個 key 一定進同一個 partition</b></li>
 * <li>同一個 partition 裡的訊息依序被讀取，所以「A 建立」一定在「A 取消」之前處理</li>
 * <li>不同 partition 之間沒有順序，但可以由不同的 consumer 同時處理（第 3 課）</li>
 * </ul>
 *
 * 所以 key 要選「需要保持順序的單位」：訂單的事件用 orderId。
 */
@SpringBootTest
class L02_PartitionAndKeyTest extends KafkaContainerTestBase {

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaTemplate;

	@Autowired
	private ShippingService shippingService;

	private int partitionOf(String key, OrderEvent event) {
		return kafkaTemplate.send("orders", key, event).join().getRecordMetadata().partition();
	}

	@Test
	void sameKey_shouldAlwaysGoToTheSamePartition() {
		String orderId = UUID.randomUUID().toString();
		Set<Integer> partitions = new HashSet<>();

		for (int i = 0; i < 10; i++) {
			partitions.add(partitionOf(orderId, OrderEvent.placed(orderId, "alice", "book", 1)));
		}

		assertThat(partitions).hasSize(1);
	}

	@Test
	void differentKeys_shouldBeSpreadOverThePartitions() {
		Set<Integer> partitions = new HashSet<>();

		for (int i = 0; i < 30; i++) {
			String orderId = UUID.randomUUID().toString();
			partitions.add(partitionOf(orderId, OrderEvent.placed(orderId, "alice", "book", 1)));
		}

		assertThat(partitions).containsExactlyInAnyOrder(0, 1, 2);
	}

	// 建立、取消用同一個 key：同一個 partition，依序處理，最後的狀態一定是「取消」
	@Test
	void eventsOfTheSameOrder_shouldBeHandledInOrder() {
		String orderId = UUID.randomUUID().toString();

		kafkaTemplate.send("orders", orderId, OrderEvent.placed(orderId, "alice", "book", 1));
		kafkaTemplate.send("orders", orderId, OrderEvent.cancelled(orderId));

		await().atMost(Duration.ofSeconds(10))
				.until(() -> shippingService.status(orderId).equals(Optional.of(ShippingService.Status.CANCELLED)));
	}
}
