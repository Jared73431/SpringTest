package com.example.demo.message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 * 留言存在記憶體中（重新啟動就會消失），這個模組的重點是伺服器端渲染，不是資料存取。
 */
@Service
public class MessageService {

	private static final int MAX_MESSAGES = 50;

	private final List<Message> messages = new CopyOnWriteArrayList<>();
	private final AtomicLong ids = new AtomicLong();

	/** 最新的在前面 */
	public List<Message> findAll() {
		return List.copyOf(messages);
	}

	// synchronized：新增與超過上限時刪除最舊的一筆，要一起完成
	public synchronized Message add(String author, String content) {
		var message = new Message(ids.incrementAndGet(), author.strip(), content.strip(), LocalDateTime.now());
		messages.addFirst(message);
		if (messages.size() > MAX_MESSAGES) {
			messages.removeLast();
		}
		return message;
	}
}
