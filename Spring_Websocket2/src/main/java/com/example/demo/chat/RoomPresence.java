package com.example.demo.chat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 每個聊天室有哪些人在線上（存在記憶體中）。
 *
 * <p>
 * 修正前存在 Redis，但斷線時沒有移除（要等 1 小時過期）。只有一台主機時放在記憶體就夠了；
 * 多台主機時才需要 Redis 這類共用的儲存（同時也需要外部的 broker）。
 *
 * <p>
 * 簡化：同一個名稱開兩個分頁，關掉其中一個就會被當成離開。
 */
@Component
public class RoomPresence {

	private final Map<String, Set<String>> usersByRoom = new ConcurrentHashMap<>();

	/** 加入聊天室，回傳目前的名單 */
	public List<String> join(String room, String user) {
		usersByRoom.computeIfAbsent(room, key -> ConcurrentHashMap.newKeySet()).add(user);
		return users(room);
	}

	/** 從所有聊天室移除，回傳他原本所在的聊天室 */
	public List<String> leaveAll(String user) {
		return usersByRoom.entrySet().stream()
				.filter(entry -> entry.getValue().remove(user))
				.map(Map.Entry::getKey)
				.toList();
	}

	public List<String> users(String room) {
		return usersByRoom.getOrDefault(room, Set.of()).stream().sorted().toList();
	}
}
