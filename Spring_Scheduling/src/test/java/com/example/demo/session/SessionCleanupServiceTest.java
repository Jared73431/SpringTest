package com.example.demo.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.PostgresContainerTestBase;

/**
 * 排程的業務邏輯直接呼叫、直接測試，不必等到凌晨 3 點。
 */
@SpringBootTest
class SessionCleanupServiceTest extends PostgresContainerTestBase {

	@Autowired
	private SessionCleanupService cleanupService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertSessions() {
		jdbcTemplate.update("TRUNCATE user_session");
		jdbcTemplate.update("INSERT INTO user_session (username, expires_at) VALUES ('amy', LOCALTIMESTAMP - INTERVAL '1 hour')");
		jdbcTemplate.update("INSERT INTO user_session (username, expires_at) VALUES ('bob', LOCALTIMESTAMP + INTERVAL '1 hour')");
	}

	@Test
	void deleteExpired_shouldDeleteOnlyExpiredSessions() {
		assertThat(cleanupService.deleteExpired()).isEqualTo(1);

		assertThat(jdbcTemplate.queryForList("SELECT username FROM user_session", String.class)).containsExactly("bob");
	}
}
