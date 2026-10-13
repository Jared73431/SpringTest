package com.example.demo.shipping;

/**
 * 業務規則不允許出貨。重試也不會成功，所以錯誤處理不重試、直接送到 Dead Letter Topic（KafkaConfig）。
 */
public class ShippingRejectedException extends RuntimeException {

	public ShippingRejectedException(String message) {
		super(message);
	}
}
