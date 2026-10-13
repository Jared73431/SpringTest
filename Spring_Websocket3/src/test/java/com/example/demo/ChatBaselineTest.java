package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Baseline：鎖定重構前的行為（STOMP 多聊天室、私訊、線上名單）。
 *
 * <p>
 * 程式沒有用到資料庫與 Redis，但依賴與設定都在：JPA 啟動時需要資料庫，所以用 Testcontainers 提供
 * （原本的 contextLoads 會連到本機的 PostgreSQL）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatBaselineTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

	static {
		POSTGRES.start();
	}

	@LocalServerPort
	private int port;

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object payload) {
		return (Map<String, Object>) payload;
	}

	private StompSession connect() throws Exception {
		var client = new WebSocketStompClient(
				new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
		client.setMessageConverter(new MappingJackson2MessageConverter());
		return client.connectAsync("http://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
		}).get(10, TimeUnit.SECONDS);
	}

	private static BlockingQueue<Object> subscribe(StompSession session, String destination, Class<?> type)
			throws InterruptedException {
		BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
		session.subscribe(destination, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return type;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				queue.add(payload);
			}
		});
		Thread.sleep(300); // 等 SUBSCRIBE 送達
		return queue;
	}

	@Test
	void join_shouldBeBroadcastToTheRoom_andUpdateTheUserList() throws Exception {
		var alice = connect();
		var room = subscribe(alice, "/topic/room/java", Map.class);
		var users = subscribe(alice, "/topic/room/java/users", List.class);

		alice.send("/app/chat/java", Map.of("type", "JOIN", "sender", "alice", "content", "alice joined"));

		assertThat(users.poll(5, TimeUnit.SECONDS)).isEqualTo(List.of("alice"));
		assertThat(asMap(room.poll(5, TimeUnit.SECONDS))).containsEntry("roomId", "java");
		alice.disconnect();
	}

	// JOIN 的內容由客戶端提供，伺服器原封不動地廣播；頁面顯示 JOIN / LEAVE 時沒有跳脫（XSS）
	@Test
	void joinContent_shouldBeBroadcastAsSentByTheClient() throws Exception {
		var alice = connect();
		var room = subscribe(alice, "/topic/room/xss", Map.class);

		alice.send("/app/chat/xss", Map.of("type", "JOIN", "sender", "alice", "content", "<img src=x onerror=alert(1)>"));

		assertThat(asMap(room.poll(5, TimeUnit.SECONDS))).containsEntry("content", "<img src=x onerror=alert(1)>");
		alice.disconnect();
	}

	// convertAndSendToUser("bob", ...) 依「登入者（Principal）的名稱」找連線；沒有登入機制，沒有任何連線屬於 bob
	@Test
	void privateMessage_shouldNeverArrive() throws Exception {
		var bob = connect();
		var bobPrivate = subscribe(bob, "/user/queue/private", Map.class);
		bob.send("/app/chat/lobby", Map.of("type", "JOIN", "sender", "bob", "content", "bob joined"));
		var alice = connect();

		alice.send("/app/chat/private", Map.of("sender", "alice", "recipient", "bob", "content", "secret"));

		assertThat(bobPrivate.poll(3, TimeUnit.SECONDS)).isNull();
		alice.disconnect();
		bob.disconnect();
	}

	// 斷線時從房間移除，廣播 LEAVE 與新的名單
	@Test
	void disconnect_shouldRemoveTheUserFromTheRoom() throws Exception {
		var watcher = connect();
		var room = subscribe(watcher, "/topic/room/leave", Map.class);
		var users = subscribe(watcher, "/topic/room/leave/users", List.class);
		var carol = connect();
		carol.send("/app/chat/leave", Map.of("type", "JOIN", "sender", "carol", "content", "carol joined"));
		assertThat(users.poll(5, TimeUnit.SECONDS)).isEqualTo(List.of("carol"));
		room.poll(5, TimeUnit.SECONDS);

		carol.disconnect();

		assertThat(asMap(room.poll(5, TimeUnit.SECONDS))).containsEntry("type", "LEAVE");
		assertThat(users.poll(5, TimeUnit.SECONDS)).isEqualTo(List.of());
		watcher.disconnect();
	}
}
