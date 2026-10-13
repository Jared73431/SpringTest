package com.example.demo.spring;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 廣播：任何一條連線送來的訊息，轉送給所有連線（第 2 課）。
 *
 * <p>
 * 兩個要注意的地方：
 * <ul>
 * <li>連線要自己記住（Map），斷線時移除，否則會一直對已經關閉的連線送訊息</li>
 * <li><b>同一條連線不能被多條執行緒同時送訊息</b>：兩個人同時發言，兩條執行緒會同時對第三個人的連線 sendMessage，
 * 拋出 IllegalStateException（TEXT_PARTIAL_WRITING）。用 ConcurrentWebSocketSessionDecorator 包起來，
 * 同時送的訊息會排隊依序送出</li>
 * </ul>
 */
@Component
public class BroadcastHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(BroadcastHandler.class);

	/** 排隊的訊息最多等多久（毫秒）、最多累積多少（bytes）：對方太慢時超過就關閉這條連線，不讓它拖累伺服器 */
	private static final int SEND_TIME_LIMIT = 5_000;

	private static final int BUFFER_SIZE_LIMIT = 1024 * 1024;

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessions.put(session.getId(),
				new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, BUFFER_SIZE_LIMIT));
	}

	@Override
	protected void handleTextMessage(WebSocketSession sender, TextMessage message) {
		var text = new TextMessage("[" + sender.getId() + "] " + message.getPayload());
		sessions.values().forEach(session -> {
			try {
				session.sendMessage(text);
			}
			catch (IOException e) {
				log.warn("broadcast to {} failed: {}", session.getId(), e.getMessage());
			}
		});
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessions.remove(session.getId());
	}

	public int connectedSessions() {
		return sessions.size();
	}
}
