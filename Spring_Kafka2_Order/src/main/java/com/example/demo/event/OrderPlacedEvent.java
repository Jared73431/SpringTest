package com.example.demo.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Order 服務送到 topic「order-events」的事件，key 是 orderId。
 *
 * <p>
 * 兩個服務之間的約定是「JSON 的欄位」，不是這個類別：Shipping 服務有自己的一份同名 record，
 * 兩邊不共用程式碼，各自獨立修改、部署（第 1 課）。
 */
public record OrderPlacedEvent(UUID eventId, UUID orderId, String customer, String item, int quantity,
		Instant occurredAt) {

	public static OrderPlacedEvent of(UUID orderId, String customer, String item, int quantity) {
		return new OrderPlacedEvent(UUID.randomUUID(), orderId, customer, item, quantity, Instant.now());
	}
}
