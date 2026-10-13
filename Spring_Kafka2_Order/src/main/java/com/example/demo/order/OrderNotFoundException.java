package com.example.demo.order;

import java.util.UUID;

/** 查詢不到訂單：HTTP 回應 404；收到不存在的訂單的回覆事件時不重試，直接送到 Dead Letter Topic */
public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(UUID orderId) {
		super("order " + orderId + " not found");
	}
}
