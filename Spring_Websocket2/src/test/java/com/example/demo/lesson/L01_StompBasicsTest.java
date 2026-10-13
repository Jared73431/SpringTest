package com.example.demo.lesson;

import static com.example.demo.lesson.ChatClient.next;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 1 課：STOMP —— 在 WebSocket 上加一層「目的地」。
 *
 * <p>
 * 原生 WebSocket（Spring_Websocket）只有「一則一則的文字」，要自己決定格式、自己記住誰在哪個聊天室。
 * STOMP 在每則訊息加上目的地（destination），Spring 依目的地分派：
 *
 * <pre>
 * SEND      /app/rooms/java/messages  → ChatController 的 @MessageMapping 處理
 * SUBSCRIBE /topic/rooms/java         → broker 把送到這裡的訊息轉給所有訂閱者
 * </pre>
 *
 * 就像 HTTP 的 @RequestMapping，只是方向是雙向的。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L01_StompBasicsTest extends PostgresContainerTestBase {

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void cleanUp() {
		jdbc.sql("DELETE FROM chat_message").update();
	}

	@Test
	void message_shouldReachEverySubscriberOfTheRoom() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var bob = ChatClient.connect(port, "bob");
		var aliceRoom = alice.subscribe("/topic/rooms/java", Map.class);
		var bobRoom = bob.subscribe("/topic/rooms/java", Map.class);

		alice.send("/app/rooms/java/messages", Map.of("content", "hi"));

		assertThat(next(bobRoom)).containsEntry("sender", "alice").containsEntry("content", "hi")
				.containsEntry("type", "CHAT");
		assertThat(next(aliceRoom)).containsEntry("content", "hi");
		alice.disconnect();
		bob.disconnect();
	}

	@Test
	void message_shouldNotReachOtherRooms() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var bob = ChatClient.connect(port, "bob");
		var bobOtherRoom = bob.subscribe("/topic/rooms/python", Map.class);

		alice.send("/app/rooms/java/messages", Map.of("content", "hi"));

		assertThat(bobOtherRoom.poll(1, TimeUnit.SECONDS)).isNull();
		alice.disconnect();
		bob.disconnect();
	}

	// 發送者由伺服器從連線的 Principal 取得：訊息裡自稱 admin 也沒用（修正前任何人都能冒充別人）
	@Test
	void sender_shouldComeFromTheConnection_notFromTheMessage() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var room = alice.subscribe("/topic/rooms/java", Map.class);

		alice.send("/app/rooms/java/messages", Map.of("sender", "admin", "content", "trust me"));

		assertThat(next(room)).containsEntry("sender", "alice");
		alice.disconnect();
	}

	// 「查過去」用 REST、「收即時」用 WebSocket
	@Test
	void history_shouldBeStoredAndReturnedOldestFirst() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var room = alice.subscribe("/topic/rooms/java", Map.class);
		alice.send("/app/rooms/java/messages", Map.of("content", "first"));
		next(room);
		alice.send("/app/rooms/java/messages", Map.of("content", "second"));
		next(room);

		List<Map<String, Object>> history = RestClient.create("http://localhost:" + port).get().uri("/api/rooms/java/messages")
				.retrieve().body(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
				});

		assertThat(history).extracting(m -> m.get("content")).containsExactly("first", "second");
		alice.disconnect();
	}

	// 沒有名稱：握手時就拒絕（400）
	@Test
	void connection_shouldBeRejected_withoutAName() {
		assertThatThrownBy(() -> ChatClient.connect(port, "")).hasMessageContaining("400");
	}
}
