package com.example.demo.order;

import java.time.Instant;
import java.util.UUID;

public record Order(UUID id, String customer, String item, int quantity, Status status, String reason,
		Instant createdAt, Instant updatedAt) {

	/**
	 * PENDING：已建立、等 Shipping 服務回覆。回覆的事件到了才變成 SHIPPED 或 REJECTED。
	 * 中間這段時間兩個服務的資料不一致，之後才一致：最終一致性（第 2 課）。
	 */
	public enum Status {
		PENDING, SHIPPED, REJECTED
	}
}
