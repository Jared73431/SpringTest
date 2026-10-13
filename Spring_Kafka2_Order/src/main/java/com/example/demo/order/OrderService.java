package com.example.demo.order;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.KafkaConfig;
import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.event.ProcessedEventRepository;
import com.example.demo.event.ShipmentEvent;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;

	private final ProcessedEventRepository processedEvents;

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public OrderService(OrderRepository orderRepository, ProcessedEventRepository processedEvents,
			KafkaTemplate<String, Object> kafkaTemplate) {
		this.orderRepository = orderRepository;
		this.processedEvents = processedEvents;
		this.kafkaTemplate = kafkaTemplate;
	}

	/**
	 * 建立訂單（PENDING）並送出 OrderPlacedEvent。
	 *
	 * <p>
	 * 「寫入資料庫」和「送到 Kafka」是兩個系統，沒辦法放在同一個交易（第 3 課）：
	 * <ul>
	 * <li>送出失敗：拋出例外 → 資料庫 rollback，訂單不存在 → 兩邊一致</li>
	 * <li>送出成功、但之後資料庫 commit 失敗：事件已經送出，訂單卻不存在 → 不一致（機率很低，Outbox pattern 解決，見 README）</li>
	 * </ul>
	 */
	@Transactional
	public Order place(String customer, String item, int quantity) {
		Order order = orderRepository.insert(UUID.randomUUID(), customer, item, quantity);
		try {
			// join()：等 Kafka 確認收到（acks=all），送不出去就讓交易 rollback
			kafkaTemplate.send(KafkaConfig.ORDER_EVENTS, order.id().toString(),
					OrderPlacedEvent.of(order.id(), customer, item, quantity)).join();
		}
		catch (CompletionException e) {
			throw new KafkaException("order event could not be sent", e.getCause());
		}
		return order;
	}

	public Optional<Order> find(UUID orderId) {
		return orderRepository.findById(orderId);
	}

	/**
	 * 收到 Shipping 服務的回覆，更新訂單狀態。
	 *
	 * <p>
	 * 「記住處理過的事件」和「更新訂單」在同一個交易：同一個事件送達兩次，第二次直接略過（第 3 課）。
	 */
	@Transactional
	public void applyShipmentResult(ShipmentEvent event) {
		if (!processedEvents.markProcessed(event.eventId())) {
			log.info("Duplicate event {} for order {}, skipped", event.eventId(), event.orderId());
			return;
		}
		Order.Status status = switch (event.status()) {
			case SHIPPED -> Order.Status.SHIPPED;
			case REJECTED -> Order.Status.REJECTED;
		};
		if (orderRepository.updateStatus(event.orderId(), status, event.reason()) == 0) {
			throw new OrderNotFoundException(event.orderId());
		}
	}
}
