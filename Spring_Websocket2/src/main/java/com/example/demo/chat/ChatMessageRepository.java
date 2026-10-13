package com.example.demo.chat;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.demo.chat.ChatMessage.Type;

/**
 * 聊天紀錄（chat_message 表）。只需要「新增」和「查最近幾筆」，用 JdbcClient 直接寫 SQL 就夠了。
 */
@Repository
public class ChatMessageRepository {

	private final JdbcClient jdbcClient;

	public ChatMessageRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public ChatMessage save(String room, String sender, String content) {
		return jdbcClient.sql("""
				INSERT INTO chat_message (room, sender, content) VALUES (:room, :sender, :content)
				RETURNING id, sent_at
				""")
				.param("room", room)
				.param("sender", sender)
				.param("content", content)
				.query((rs, rowNum) -> new ChatMessage(rs.getLong("id"), room, Type.CHAT, sender, content,
						rs.getTimestamp("sent_at").toInstant()))
				.single();
	}

	/** 最近 limit 筆，舊的在前（畫面由上往下顯示） */
	public List<ChatMessage> findRecent(String room, int limit) {
		return jdbcClient.sql("""
				SELECT * FROM (
				    SELECT id, room, sender, content, sent_at FROM chat_message
				    WHERE room = :room ORDER BY id DESC LIMIT :limit
				) recent ORDER BY id
				""")
				.param("room", room)
				.param("limit", limit)
				.query((rs, rowNum) -> new ChatMessage(rs.getLong("id"), rs.getString("room"), Type.CHAT,
						rs.getString("sender"), rs.getString("content"), rs.getTimestamp("sent_at").toInstant()))
				.list();
	}
}
