package com.example.demo.shipment;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.demo.config.KafkaConfig;
import com.example.demo.event.OrderPlacedEvent;

/**
 * 接收 Order 服務的訂單事件。consumer group 用服務名稱：之後 Shipping 服務開好幾個實例時，同一筆訂單只有一個實例處理。
 */
@Component
public class OrderPlacedListener {

	public static final String GROUP = "shipping-service";

	private final ShippingService shippingService;

	public OrderPlacedListener(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	@KafkaListener(topics = KafkaConfig.ORDER_EVENTS, groupId = GROUP)
	public void onOrderPlaced(OrderPlacedEvent event) {
		shippingService.handle(event);
	}
}
