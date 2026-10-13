package com.example.demo.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Shipping 服務回覆的事件（topic「shipment-events」，key 是 orderId）：出貨了，或拒絕出貨與原因。
 */
public record ShipmentEvent(UUID eventId, UUID orderId, Status status, String reason, Instant occurredAt) {

	public enum Status {
		SHIPPED, REJECTED
	}

	public static ShipmentEvent of(UUID orderId, Status status, String reason) {
		return new ShipmentEvent(UUID.randomUUID(), orderId, status, reason, Instant.now());
	}
}
