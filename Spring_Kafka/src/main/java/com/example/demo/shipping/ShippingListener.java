package com.example.demo.shipping;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.demo.config.KafkaConfig;
import com.example.demo.order.OrderEvent;

/**
 * consumer group「shipping」：出貨部門。
 *
 * <p>
 * concurrency = 3：同一個 group 開 3 個 consumer，各自分到 orders 的一個 partition，平行處理（第 3 課）。
 * 再多開也沒用：一個 partition 同時只會分給 group 裡的一個 consumer。
 */
@Component
public class ShippingListener {

	public static final String GROUP = "shipping";

	private final ShippingService shippingService;

	public ShippingListener(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	// 方法正常結束 → Spring 替我們 commit offset（「這則處理完了」）；拋出例外 → 交給 KafkaConfig 的錯誤處理
	@KafkaListener(id = "shippingListener", topics = KafkaConfig.ORDERS_TOPIC, groupId = GROUP, concurrency = "3")
	public void onOrderEvent(OrderEvent event) {
		shippingService.handle(event);
	}
}
