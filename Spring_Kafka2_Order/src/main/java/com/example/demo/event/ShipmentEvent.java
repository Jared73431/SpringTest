package com.example.demo.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Shipping 服務回覆的事件（topic「shipment-events」，key 是 orderId）。
 *
 * <p>
 * 這是 Order 服務自己的一份：只要 JSON 欄位對得上，就能轉成這個 record。
 */
public record ShipmentEvent(UUID eventId, UUID orderId, Status status, String reason, Instant occurredAt) {

	public enum Status {
		SHIPPED, REJECTED
	}
}
