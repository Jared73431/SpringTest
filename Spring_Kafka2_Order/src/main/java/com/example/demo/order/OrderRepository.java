package com.example.demo.order;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.demo.order.Order.Status;

@Repository
public class OrderRepository {

	private final JdbcClient jdbcClient;

	public OrderRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public Order insert(UUID id, String customer, String item, int quantity) {
		return jdbcClient.sql("""
				INSERT INTO orders (id, customer, item, quantity, status) VALUES (:id, :customer, :item, :quantity, 'PENDING')
				RETURNING *
				""")
				.param("id", id)
				.param("customer", customer)
				.param("item", item)
				.param("quantity", quantity)
				.query(OrderRepository::toOrder)
				.single();
	}

	public Optional<Order> findById(UUID id) {
		return jdbcClient.sql("SELECT * FROM orders WHERE id = :id")
				.param("id", id)
				.query(OrderRepository::toOrder)
				.optional();
	}

	/** 回傳更新的筆數：0 表示沒有這筆訂單 */
	public int updateStatus(UUID id, Status status, String reason) {
		return jdbcClient.sql("UPDATE orders SET status = :status, reason = :reason, updated_at = now() WHERE id = :id")
				.param("id", id)
				.param("status", status.name())
				.param("reason", reason)
				.update();
	}

	private static Order toOrder(ResultSet rs, int rowNum) throws SQLException {
		return new Order(rs.getObject("id", UUID.class), rs.getString("customer"), rs.getString("item"),
				rs.getInt("quantity"), Status.valueOf(rs.getString("status")), rs.getString("reason"),
				rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
	}
}
