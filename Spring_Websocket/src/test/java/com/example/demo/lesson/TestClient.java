package com.example.demo.lesson;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** 課程共用的 WebSocket 客戶端：記下收到的訊息與關閉狀態 */
final class TestClient {

	final BlockingQueue<String> received = new LinkedBlockingQueue<>();

	final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();

	WebSocketSession session;

	static TestClient connect(int port, String path) throws Exception {
		return connect(port, path, new WebSocketHttpHeaders());
	}

	static TestClient connect(int port, String path, WebSocketHttpHeaders headers) throws Exception {
		var client = new TestClient();
		client.session = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			@Override
			protected void handleTextMessage(WebSocketSession session, TextMessage message) {
				client.received.add(message.getPayload());
			}

			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
				client.closed.complete(status);
			}
		}, headers, URI.create("ws://localhost:" + port + path)).get(5, TimeUnit.SECONDS);
		client.session.setTextMessageSizeLimit(1024 * 1024);
		return client;
	}

	void send(String text) throws Exception {
		session.sendMessage(new TextMessage(text));
	}

	String next() throws InterruptedException {
		return received.poll(5, TimeUnit.SECONDS);
	}

	void close() throws Exception {
		session.close();
	}
}
