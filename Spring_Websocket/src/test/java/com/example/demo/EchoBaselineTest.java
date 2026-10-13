package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Baseline：鎖定重構前的行為（Jakarta @ServerEndpoint 的 Echo）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EchoBaselineTest {

	@LocalServerPort
	private int port;

	private final BlockingQueue<String> received = new LinkedBlockingQueue<>();

	private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();

	private WebSocketSession connect() throws Exception {
		return new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			@Override
			protected void handleTextMessage(WebSocketSession session, TextMessage message) {
				received.add(message.getPayload());
			}

			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
				closed.complete(status);
			}
		}, "ws://localhost:" + port + "/channel/echo").get(5, TimeUnit.SECONDS);
	}

	@Test
	void message_shouldBeAnsweredWithATimestampAndHello() throws Exception {
		var session = connect();

		session.sendMessage(new TextMessage("Jared"));

		assertThat(received.poll(5, TimeUnit.SECONDS)).matches("\\[\\d{13}\\] Hello Jared");
		session.close();
	}

	// 送出 bye：由伺服器關閉連線，狀態碼 1000（NORMAL_CLOSURE），原因 Bye
	@Test
	void bye_shouldMakeTheServerCloseTheConnection() throws Exception {
		var session = connect();

		session.sendMessage(new TextMessage("BYE"));

		var status = closed.get(5, TimeUnit.SECONDS);
		assertThat(status.getCode()).isEqualTo(1000);
		assertThat(status.getReason()).isEqualTo("Bye");
		assertThat(received).isEmpty();
	}
}
