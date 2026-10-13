package com.example.demo.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Order 服務送來的事件（topic「order-events」，key 是 orderId）。
 *
 * <p>
 * 這是 Shipping 服務自己的一份：只要 JSON 欄位對得上，就能轉成這個 record，不需要 Order 服務的程式碼。
 */
public record OrderPlacedEvent(UUID eventId, UUID orderId, String customer, String item, int quantity,
		Instant occurredAt) {
}
