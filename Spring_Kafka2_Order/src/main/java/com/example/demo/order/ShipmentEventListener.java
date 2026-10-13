package com.example.demo.order;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.demo.config.KafkaConfig;
import com.example.demo.event.ShipmentEvent;

/**
 * 接收 Shipping 服務的回覆。consumer group 用服務名稱：之後 Order 服務開好幾個實例時，同一則回覆只有一個實例處理。
 */
@Component
public class ShipmentEventListener {

	public static final String GROUP = "order-service";

	private final OrderService orderService;

	public ShipmentEventListener(OrderService orderService) {
		this.orderService = orderService;
	}

	@KafkaListener(topics = KafkaConfig.SHIPMENT_EVENTS, groupId = GROUP)
	public void onShipmentEvent(ShipmentEvent event) {
		orderService.applyShipmentResult(event);
	}
}
