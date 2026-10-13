package com.example.demo.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.demo.config.KafkaConfig;
import com.example.demo.order.OrderEvent;

/**
 * consumer group「notification」：通知顧客。
 *
 * <p>
 * 和出貨是<b>不同的 group</b>，所以每個事件出貨收到一次、通知也收到一次（第 3 課）。
 * 送出訂單的一方不需要知道有誰在處理：之後新增「積點」部門，只要再加一個 group，不必修改訂單的程式。
 */
@Component
public class NotificationListener {

	private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

	public static final String GROUP = "notification";

	private final List<String> sent = new CopyOnWriteArrayList<>();

	@KafkaListener(topics = KafkaConfig.ORDERS_TOPIC, groupId = GROUP)
	public void onOrderEvent(OrderEvent event) {
		String message = switch (event.type()) {
			case PLACED -> event.customer() + ", your order " + event.orderId() + " has been placed";
			case CANCELLED -> "Order " + event.orderId() + " has been cancelled";
		};
		sent.add(message);
		log.info("Notification: {}", message);
	}

	public List<String> sent() {
		return List.copyOf(sent);
	}
}
