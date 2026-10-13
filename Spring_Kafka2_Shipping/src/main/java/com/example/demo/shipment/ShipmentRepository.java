package com.example.demo.shipment;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.demo.event.ShipmentEvent.Status;

@Repository
public class ShipmentRepository {

	private final JdbcClient jdbcClient;

	public ShipmentRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public void insert(UUID orderId, Status status, String reason) {
		jdbcClient.sql("INSERT INTO shipment (order_id, status, reason) VALUES (:orderId, :status, :reason)")
				.param("orderId", orderId)
				.param("status", status.name())
				.param("reason", reason)
				.update();
	}

	public Optional<Shipment> findByOrderId(UUID orderId) {
		return jdbcClient.sql("SELECT * FROM shipment WHERE order_id = :orderId")
				.param("orderId", orderId)
				.query((rs, rowNum) -> new Shipment(rs.getObject("order_id", UUID.class),
						Status.valueOf(rs.getString("status")), rs.getString("reason"),
						rs.getTimestamp("created_at").toInstant()))
				.optional();
	}
}
