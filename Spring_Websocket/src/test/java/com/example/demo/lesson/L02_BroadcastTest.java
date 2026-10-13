package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.demo.spring.BroadcastHandler;

/**
 * 第 2 課：廣播 —— 伺服器主動把訊息送給所有連線。
 *
 * <ul>
 * <li>連線要自己記住，斷線時移除</li>
 * <li>同一條連線不能被多條執行緒同時送訊息：用 ConcurrentWebSocketSessionDecorator 讓訊息排隊</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(L02_BroadcastTest.RawSessionConfig.class)
class L02_BroadcastTest {

	/** 測試用：記下伺服器端「沒有包裝」的連線，用來示範多執行緒同時送訊息會失敗 */
	static final AtomicReference<WebSocketSession> RAW_SESSION = new AtomicReference<>();

	@TestConfiguration
	@EnableWebSocket
	static class RawSessionConfig implements WebSocketConfigurer {

		@Override
		public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
			registry.addHandler(new TextWebSocketHandler() {
				@Override
				public void afterConnectionEstablished(WebSocketSession session) {
					RAW_SESSION.set(session);
				}
			}, "/test/raw");
		}
	}

	@LocalServerPort
	private int port;

	@Autowired
	private BroadcastHandler broadcastHandler;

	@Test
	void message_shouldBeSentToEveryConnection() throws Exception {
		var alice = TestClient.connect(port, "/ws/spring/broadcast");
		var bob = TestClient.connect(port, "/ws/spring/broadcast");
		var carol = TestClient.connect(port, "/ws/spring/broadcast");

		alice.send("hi");

		for (var client : new TestClient[] { alice, bob, carol }) {
			assertThat(client.next()).endsWith("] hi");
		}
		alice.close();
		bob.close();
		carol.close();
	}

	@Test
	void closedConnection_shouldBeForgotten() throws Exception {
		int before = broadcastHandler.connectedSessions();
		var client = TestClient.connect(port, "/ws/spring/broadcast");
		await().atMost(Duration.ofSeconds(5)).until(() -> broadcastHandler.connectedSessions() == before + 1);

		client.close();

		await().atMost(Duration.ofSeconds(5)).until(() -> broadcastHandler.connectedSessions() == before);
	}

	/** 8 條執行緒同時對同一條連線送 40 則訊息，回傳失敗的次數 */
	private static int sendConcurrently(WebSocketSession session, int size) throws Exception {
		var errors = new ConcurrentLinkedQueue<Throwable>();
		var text = new TextMessage("x".repeat(size));
		var pool = Executors.newFixedThreadPool(8);
		for (int i = 0; i < 40; i++) {
			pool.submit(() -> {
				try {
					session.sendMessage(text);
				}
				catch (Throwable e) {
					errors.add(e);
				}
			});
		}
		pool.shutdown();
		pool.awaitTermination(30, TimeUnit.SECONDS);
		return errors.size();
	}

	// 沒有包裝：一則訊息還沒送完，另一條執行緒又開始送 → IllegalStateException（TEXT_PARTIAL_WRITING）
	@Test
	void concurrentSends_shouldFail_onARawSession() throws Exception {
		var client = TestClient.connect(port, "/test/raw");
		await().until(() -> RAW_SESSION.get() != null);

		assertThat(sendConcurrently(RAW_SESSION.get(), 500_000)).isPositive();
		client.close();
	}

	// 包裝之後：同時送的訊息排隊依序送出，全部成功
	@Test
	void concurrentSends_shouldAllArrive_withTheDecorator() throws Exception {
		RAW_SESSION.set(null);
		var client = TestClient.connect(port, "/test/raw");
		await().until(() -> RAW_SESSION.get() != null);
		var decorated = new ConcurrentWebSocketSessionDecorator(RAW_SESSION.get(), 10_000, 10 * 1024 * 1024);

		assertThat(sendConcurrently(decorated, 20_000)).isZero();
		await().atMost(Duration.ofSeconds(10)).until(() -> client.received.size() == 40);
		client.close();
	}
}
