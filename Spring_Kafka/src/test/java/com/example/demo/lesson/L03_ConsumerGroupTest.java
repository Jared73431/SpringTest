package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import com.example.demo.KafkaContainerTestBase;
import com.example.demo.notification.NotificationListener;
import com.example.demo.order.OrderEvent;
import com.example.demo.shipping.ShippingService;

/**
 * 第 3 課：Consumer group —— 分工與廣播。
 *
 * <pre>
 *                       topic「orders」（3 個 partition）
 *                     ┌───────────┼───────────┐
 * group「shipping」   consumer 1   consumer 2   consumer 3   ← 同一個 group：分工，每則訊息只處理一次
 * group「notification」          consumer 1（3 個 partition 都給它）
 *                                              ← 不同的 group：各自收到全部的訊息
 * </pre>
 *
 * 一個 partition 同時只分給 group 裡的一個 consumer：partition 數量就是一個 group 平行處理的上限。
 */
@SpringBootTest
class L03_ConsumerGroupTest extends KafkaContainerTestBase {

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaTemplate;

	@Autowired
	private ShippingService shippingService;

	@Autowired
	private NotificationListener notifications;

	@Autowired
	private KafkaListenerEndpointRegistry registry;

	@Test
	void everyGroup_shouldReceiveEveryEvent() {
		String orderId = UUID.randomUUID().toString();

		kafkaTemplate.send("orders", orderId, OrderEvent.placed(orderId, "carol", "cup", 1));

		await().atMost(Duration.ofSeconds(10))
				.until(() -> shippingService.status(orderId).equals(Optional.of(ShippingService.Status.WAITING_FOR_SHIPMENT)));
		await().atMost(Duration.ofSeconds(10))
				.until(() -> notifications.sent().contains("carol, your order " + orderId + " has been placed"));
	}

	// shipping 有 3 個 consumer（concurrency = 3），每個分到 1 個 partition
	@Test
	void consumersInTheSameGroup_shouldShareThePartitions() {
		var container = (ConcurrentMessageListenerContainer<?, ?>) registry.getListenerContainer("shippingListener");

		await().atMost(Duration.ofSeconds(10)).until(() -> container.getAssignedPartitions().size() == 3);

		assertThat(container.getContainers()).hasSize(3)
				.allSatisfy(consumer -> assertThat(consumer.getAssignedPartitions()).hasSize(1));
		assertThat(container.getAssignedPartitions()).extracting(TopicPartition::partition)
				.containsExactlyInAnyOrder(0, 1, 2);
	}
}
