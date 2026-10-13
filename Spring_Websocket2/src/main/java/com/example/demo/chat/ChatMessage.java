package com.example.demo.chat;

import java.time.Instant;

/**
 * 廣播到 /topic/rooms/{room} 的訊息。CHAT 存進資料庫；JOIN、LEAVE 是系統訊息，不存（id 為 null）。
 */
public record ChatMessage(Long id, String room, Type type, String sender, String content, Instant sentAt) {

	public enum Type {
		CHAT, JOIN, LEAVE
	}

	static ChatMessage system(String room, Type type, String user) {
		String content = user + (type == Type.JOIN ? " joined" : " left");
		return new ChatMessage(null, room, type, user, content, Instant.now());
	}
}
