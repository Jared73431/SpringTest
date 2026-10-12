package com.example.demo.notification;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 代替真正的 Email / 推播：記錄發送內容並寫 log。Job 中會注入這個 Spring Bean。
 */
@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final List<Notification> sent = new CopyOnWriteArrayList<>();

	public void send(String recipient, String message, String sentBy) {
		sent.add(new Notification(recipient, message, Instant.now(), sentBy));
		log.info("[{}] Send to {}: {}", sentBy, recipient, message);
	}

	public List<Notification> sent() {
		return List.copyOf(sent);
	}

	public void clear() {
		sent.clear();
	}
}
