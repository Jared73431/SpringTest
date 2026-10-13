package com.example.demo.jakarta;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Jakarta WebSocket（Java 標準 API）的 Echo：收到 "名字" 回覆 "[時間] Hello 名字"，收到 bye 由伺服器關閉連線。
 *
 * <p>
 * <b>每條連線建立一個新的物件</b>，所以可以把 Session 存在欄位裡。Spring 的 WebSocketHandler 剛好相反，
 * 所有連線共用一個物件（見 spring.EchoHandler）。
 *
 * <p>
 * 這個類別不是 Spring Bean，由 Tomcat 建立；需要 ServerEndpointExporter 註冊（JakartaEndpointConfig）。
 */
@ServerEndpoint("/ws/jakarta/echo")
public class EchoEndpoint {

	private static final Logger log = LoggerFactory.getLogger(EchoEndpoint.class);

	/** 建立過幾個物件（第 1 課：每條連線一個） */
	public static final AtomicInteger INSTANCES = new AtomicInteger();

	private Session session;

	public EchoEndpoint() {
		INSTANCES.incrementAndGet();
	}

	@OnOpen
	public void onOpen(Session session) {
		this.session = session;
		log.info("[jakarta] connected: {}", session.getId());
	}

	@OnMessage
	public void onMessage(String message) throws IOException {
		if (message.equalsIgnoreCase("bye")) {
			// 由伺服器主動關閉，狀態碼 1000（NORMAL_CLOSURE）
			session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Bye"));
			return;
		}
		session.getAsyncRemote().sendText("[" + Instant.now().toEpochMilli() + "] Hello " + message);
	}

	@OnClose
	public void onClose(CloseReason reason) {
		log.info("[jakarta] closed: {} {}", session.getId(), reason);
	}

	@OnError
	public void onError(Throwable error) throws IOException {
		log.warn("[jakarta] error: {} {}", session.getId(), error.getMessage());
		// 狀態碼 1011（UNEXPECTED_CONDITION）：伺服器發生預期外的錯誤
		session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Unexpected error"));
	}
}
