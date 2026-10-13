package com.example.demo.order;

import java.time.Instant;
import java.util.UUID;

/**
 * 送到 topic「orders」的事件。key 是 orderId：同一筆訂單的事件一定進同一個 partition，依序處理（第 2 課）。
 *
 * <p>
 * eventId 每個事件都不同：同一個事件被送達兩次時，consumer 用它判斷「處理過了」（第 4 課）。
 */
public record OrderEvent(String eventId, String orderId, Type type, String customer, String item, int quantity,
		Instant occurredAt) {

	public enum Type {
		PLACED, CANCELLED
	}

	public static OrderEvent placed(String orderId, String customer, String item, int quantity) {
		return new OrderEvent(UUID.randomUUID().toString(), orderId, Type.PLACED, customer, item, quantity,
				Instant.now());
	}

	public static OrderEvent cancelled(String orderId) {
		return new OrderEvent(UUID.randomUUID().toString(), orderId, Type.CANCELLED, null, null, 0, Instant.now());
	}
}
