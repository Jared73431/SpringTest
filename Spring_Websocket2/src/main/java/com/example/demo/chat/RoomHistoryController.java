package com.example.demo.chat;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 進入聊天室時，先用一般的 HTTP 取得最近的訊息，再訂閱 /topic/rooms/{room} 收新的訊息。
 * 「查過去」用 REST、「收即時」用 WebSocket，各做擅長的事。
 */
@RestController
public class RoomHistoryController {

	private static final int MAX_LIMIT = 100;

	private final ChatMessageRepository repository;

	public RoomHistoryController(ChatMessageRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/api/rooms/{room}/messages")
	public List<ChatMessage> recent(@PathVariable String room, @RequestParam(defaultValue = "50") int limit) {
		return repository.findRecent(room, Math.clamp(limit, 1, MAX_LIMIT));
	}
}
