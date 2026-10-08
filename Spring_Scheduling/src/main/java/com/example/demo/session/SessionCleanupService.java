package com.example.demo.session;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 排程「要做的事」：和「什麼時候做」（SessionCleanupTask）分開，業務邏輯可以直接呼叫、直接測試，不必等排程觸發。
 */
@Service
public class SessionCleanupService {

	private final JdbcTemplate jdbcTemplate;

	public SessionCleanupService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/** 刪除已過期的 session，回傳刪除的筆數 */
	public int deleteExpired() {
		return jdbcTemplate.update("DELETE FROM user_session WHERE expires_at < LOCALTIMESTAMP");
	}
}
