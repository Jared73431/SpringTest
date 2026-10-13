package com.example.demo.shipment;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * <pre>
 * GET /api/shipments/{orderId}    查詢某筆訂單的出貨結果
 * </pre>
 *
 * Shipping 服務沒有「建立出貨」的 API：出貨只由 order-events 的事件觸發。
 */
@RestController
public class ShipmentController {

	private final ShippingService shippingService;

	public ShipmentController(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	@GetMapping("/api/shipments/{orderId}")
	public Shipment find(@PathVariable UUID orderId) {
		return shippingService.find(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "shipment for order " + orderId + " not found"));
	}
}
