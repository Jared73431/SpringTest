package com.example.demo.shipment;

import java.time.Instant;
import java.util.UUID;

import com.example.demo.event.ShipmentEvent.Status;

public record Shipment(UUID orderId, Status status, String reason, Instant createdAt) {
}
