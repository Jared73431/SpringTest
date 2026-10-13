package com.example.demo.event;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * 記住處理過的事件（processed_event 表），讓 consumer 冪等（第 3 課）。
 *
 * <p>
 * 必須在「更新業務資料」的同一個交易中呼叫：業務資料更新失敗而 rollback 時，這筆紀錄也一起消失，
 * 重試時才會再處理一次。
 */
@Repository
public class ProcessedEventRepository {

	private final JdbcClient jdbcClient;

	public ProcessedEventRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	/** 第一次看到這個事件回傳 true；已經處理過回傳 false */
	public boolean markProcessed(UUID eventId) {
		return jdbcClient.sql("INSERT INTO processed_event (event_id) VALUES (:eventId) ON CONFLICT DO NOTHING")
				.param("eventId", eventId)
				.update() == 1;
	}
}
