package com.example.demo.shipment;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.KafkaConfig;
import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.event.ProcessedEventRepository;
import com.example.demo.event.ShipmentEvent;
import com.example.demo.event.ShipmentEvent.Status;

@Service
public class ShippingService {

	private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

	// 一次最多出貨 10 件，超過就拒絕
	static final int MAX_QUANTITY = 10;

	private final ShipmentRepository shipmentRepository;

	private final ProcessedEventRepository processedEvents;

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public ShippingService(ShipmentRepository shipmentRepository, ProcessedEventRepository processedEvents,
			KafkaTemplate<String, Object> kafkaTemplate) {
		this.shipmentRepository = shipmentRepository;
		this.processedEvents = processedEvents;
		this.kafkaTemplate = kafkaTemplate;
	}

	/**
	 * 收到訂單 → 決定出貨或拒絕 → 寫入 shipment → 回覆 ShipmentEvent。
	 *
	 * <p>
	 * 全部在同一個資料庫交易中，回覆送不出去就拋出例外：
	 * 資料庫 rollback（連「處理過」的紀錄也一起消失），Kafka 不 commit offset，重試時從頭再處理一次（第 3 課）。
	 */
	@Transactional
	public void handle(OrderPlacedEvent event) {
		if (!processedEvents.markProcessed(event.eventId())) {
			log.info("Duplicate event {} for order {}, skipped", event.eventId(), event.orderId());
			return;
		}
		// 拒絕是正常的業務結果：回覆 REJECTED 讓 Order 服務知道，而不是拋出例外（第 2 課）
		ShipmentEvent reply = event.quantity() > MAX_QUANTITY
				? ShipmentEvent.of(event.orderId(), Status.REJECTED,
						"quantity " + event.quantity() + " exceeds " + MAX_QUANTITY)
				: ShipmentEvent.of(event.orderId(), Status.SHIPPED, null);

		shipmentRepository.insert(reply.orderId(), reply.status(), reply.reason());
		kafkaTemplate.send(KafkaConfig.SHIPMENT_EVENTS, event.orderId().toString(), reply).join();
	}

	public Optional<Shipment> find(UUID orderId) {
		return shipmentRepository.findByOrderId(orderId);
	}
}
