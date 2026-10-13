package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Baseline：鎖定重構前的行為（STOMP 公共聊天室，訊息存 PostgreSQL，線上名單存 Redis）。
 *
 * <p>
 * 原本的 contextLoads 會連到本機的 PostgreSQL（test）與 Redis，改用 Testcontainers。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatBaselineTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

	@ServiceConnection(name = "redis")
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	static {
		POSTGRES.start();
		REDIS.start();
	}

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private StringRedisTemplate redis;

	private final BlockingQueue<Map<String, Object>> publicTopic = new LinkedBlockingQueue<>();

	private StompSession session;

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object payload) {
		return (Map<String, Object>) payload;
	}

	@BeforeEach
	void connectAndSubscribe() throws Exception {
		jdbc.update("delete from chat_messages");
		redis.getConnectionFactory().getConnection().serverCommands().flushAll();
		var client = new WebSocketStompClient(
				new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
		var converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
		client.setMessageConverter(converter);
		session = client.connectAsync("http://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
		}).get(10, TimeUnit.SECONDS);
		session.subscribe("/topic/public", new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Map.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				publicTopic.add(asMap(payload));
			}
		});
		Thread.sleep(300); // 等 SUBSCRIBE 送達
	}

	@Test
	void joinAndChat_shouldBeBroadcastAndStored() throws Exception {
		// 先等 JOIN 廣播回來再送 CHAT：客戶端送來的訊息由執行緒池處理，連續送出的兩則訊息不保證依序處理
		session.send("/app/chat.addUser", Map.of("sender", "alice", "type", "JOIN", "content", "ignored"));
		assertThat(publicTopic.poll(5, TimeUnit.SECONDS)).containsEntry("content", "alice 加入了聊天室！");
		session.send("/app/chat.sendMessage", Map.of("sender", "alice", "type", "CHAT", "content", "hi"));

		assertThat(publicTopic.poll(5, TimeUnit.SECONDS)).containsEntry("content", "hi").containsEntry("sender", "alice");
		assertThat(jdbc.queryForList("select message_type from chat_messages order by id", String.class))
				.containsExactly("JOIN", "CHAT");
	}

	// sender 由客戶端自己填：任何人都能冒充別人；內容原封不動地廣播（頁面用 innerHTML 顯示 → XSS）
	@Test
	void senderAndContent_shouldBeTakenFromTheClientAsIs() throws Exception {
		session.send("/app/chat.sendMessage",
				Map.of("sender", "admin", "type", "CHAT", "content", "<img src=x onerror=alert(1)>"));

		assertThat(publicTopic.poll(5, TimeUnit.SECONDS))
				.containsEntry("sender", "admin")
				.containsEntry("content", "<img src=x onerror=alert(1)>");
	}

	// @Cacheable 使用 Redis 預設的 Java 序列化，ChatMessage 沒有實作 Serializable：有訊息時 NotSerializableException → 500
	@Test
	void recentMessages_shouldFail_whenTheRoomHasMessages() throws Exception {
		session.send("/app/chat.sendMessage", Map.of("sender", "alice", "type", "CHAT", "content", "hi"));
		publicTopic.poll(5, TimeUnit.SECONDS);

		assertThat(rest.getForEntity("/api/chat/recent/public", String.class).getStatusCode())
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	// 聊天室還沒有訊息時，空的 List 可以序列化，被快取起來。新增訊息時清除的是 recent_messages:public，
	// 但快取的 key 是 recentMessages::public：之後查到的一直是過期的空 List（10 分鐘後才過期）
	@Test
	void recentMessages_shouldStayEmpty_whenTheEmptyListWasCachedFirst() throws Exception {
		assertThat(rest.getForEntity("/api/chat/recent/public", String.class).getBody()).isEqualTo("[]");

		session.send("/app/chat.sendMessage", Map.of("sender", "alice", "type", "CHAT", "content", "hi"));
		publicTopic.poll(5, TimeUnit.SECONDS);

		assertThat(jdbc.queryForObject("select count(*) from chat_messages", Integer.class)).isEqualTo(1);
		assertThat(rest.getForEntity("/api/chat/recent/public", String.class).getBody()).isEqualTo("[]");
		assertThat(redis.keys("*")).contains("recentMessages::public");
	}

	// 斷線時只廣播 LEAVE，沒有從 Redis 的線上名單移除（要等 1 小時過期）
	@Test
	void disconnectedUser_shouldStayInTheOnlineList() throws Exception {
		session.send("/app/chat.addUser", Map.of("sender", "carol", "type", "JOIN", "content", "ignored"));
		publicTopic.poll(5, TimeUnit.SECONDS);

		session.disconnect();
		Thread.sleep(1500);

		assertThat(rest.getForEntity("/api/chat/online/public", String.class).getBody()).contains("carol");
	}

	// setAllowedOriginPatterns("*")：任何網站的頁面都能連進來
	@Test
	void connection_shouldBeAccepted_fromAnyOrigin() throws Exception {
		var headers = new WebSocketHttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://evil.example");

		var ws = new StandardWebSocketClient().execute(new TextWebSocketHandler(), headers,
				URI.create("ws://localhost:" + port + "/ws/123/abc/websocket")).get(5, TimeUnit.SECONDS);

		assertThat(ws.isOpen()).isTrue();
		ws.close();
	}
}
