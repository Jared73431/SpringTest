package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Baseline：鎖定合併前 Spring_Websocket4 的行為（Spring WebSocketHandler 聊天室與檔案上傳）。
 *
 * <p>
 * 原本連到本機的 Flyway 資料庫（其他模組的資料庫）、上傳到 D:/uploads：測試改用 Testcontainers 與暫存目錄。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ChatBaselineTest {

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static final Path UPLOAD_DIR;

	static {
		POSTGRES.start();
		try {
			UPLOAD_DIR = Files.createTempDirectory("ws4-uploads");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@DynamicPropertySource
	static void uploadDir(DynamicPropertyRegistry registry) {
		registry.add("file.upload.dir", UPLOAD_DIR::toString);
	}

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ApplicationContext context;

	/** 用 ?username= 連線（身分由客戶端自己決定），回傳收到的訊息 */
	private BlockingQueue<Map<String, Object>> connect(String username, WebSocketHttpHeaders headers,
			List<WebSocketSession> sessions) throws Exception {
		BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
		sessions.add(new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			@Override
			@SuppressWarnings("unchecked")
			protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
				received.add(objectMapper.readValue(message.getPayload(), Map.class));
			}
		}, headers, URI.create("ws://localhost:" + port + "/ws/chat?username=" + username)).get(5, TimeUnit.SECONDS));
		return received;
	}

	private static Map<String, Object> nextChatMessage(BlockingQueue<Map<String, Object>> queue) throws Exception {
		for (Map<String, Object> m = queue.poll(5, TimeUnit.SECONDS); m != null; m = queue.poll(5, TimeUnit.SECONDS)) {
			if (!"userStatus".equals(m.get("type"))) {
				return m;
			}
		}
		return null;
	}

	private String firstRoomId() throws Exception {
		List<?> rooms = objectMapper.readValue(rest.getForObject("/api/chat/rooms", String.class), List.class);
		return (String) ((Map<?, ?>) rooms.get(0)).get("roomId");
	}

	@Test
	void roomMessage_shouldBeBroadcastToEveryone_andTheSenderCanBeAnyone() throws Exception {
		var sessions = new java.util.ArrayList<WebSocketSession>();
		var alice = connect("alice", new WebSocketHttpHeaders(), sessions);
		var bob = connect("bob", new WebSocketHttpHeaders(), sessions);
		String roomId = firstRoomId();

		// alice 的連線，訊息卻說自己是 bob：伺服器照單全收
		sessions.get(0).sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
				"messageType", "CHAT", "content", "hi", "senderUsername", "bob", "roomId", roomId, "isPrivate", false))));

		assertThat(nextChatMessage(bob)).containsEntry("content", "hi").containsEntry("senderUsername", "bob");
		assertThat(nextChatMessage(alice)).containsEntry("content", "hi");
		for (var s : sessions) {
			s.close();
		}
	}

	@Test
	void privateMessage_shouldReachOnlyTheRecipientAndTheSender() throws Exception {
		var sessions = new java.util.ArrayList<WebSocketSession>();
		var dave = connect("dave", new WebSocketHttpHeaders(), sessions);
		var erin = connect("erin", new WebSocketHttpHeaders(), sessions);
		var frank = connect("frank", new WebSocketHttpHeaders(), sessions);

		sessions.get(0).sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
				"messageType", "CHAT", "content", "secret", "senderUsername", "dave", "recipientUsername", "erin",
				"isPrivate", true))));

		assertThat(nextChatMessage(erin)).containsEntry("content", "secret");
		assertThat(nextChatMessage(dave)).containsEntry("content", "secret");
		assertThat(nextChatMessage(frank)).isNull();
		for (var s : sessions) {
			s.close();
		}
	}

	// 缺少 messageType：MessageType.valueOf(null) 拋出例外，handleMessage 只寫 log 就吞掉，發送者完全不知道訊息沒送出
	@Test
	void invalidMessage_shouldFailSilently() throws Exception {
		var sessions = new java.util.ArrayList<WebSocketSession>();
		var grace = connect("grace", new WebSocketHttpHeaders(), sessions);

		sessions.get(0).sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
				"content", "hi", "senderUsername", "grace", "roomId", firstRoomId(), "isPrivate", false))));

		assertThat(nextChatMessage(grace)).isNull();
		assertThat(sessions.get(0).isOpen()).isTrue();
		sessions.get(0).close();
	}

	// setAllowedOrigins("*")：任何網站的頁面都能連進來
	@Test
	void connection_shouldBeAccepted_fromAnyOrigin() throws Exception {
		var headers = new WebSocketHttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://evil.example");
		var sessions = new java.util.ArrayList<WebSocketSession>();

		connect("mallory", headers, sessions);

		assertThat(sessions.get(0).isOpen()).isTrue();
		sessions.get(0).close();
	}

	@Test
	void uploadedFile_shouldBeDownloadable() throws Exception {
		var body = new LinkedMultiValueMap<String, Object>();
		var fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.TEXT_PLAIN);
		body.add("file", new HttpEntity<>(new ByteArrayResource("hello file".getBytes(StandardCharsets.UTF_8)) {
			@Override
			public String getFilename() {
				return "hello.txt";
			}
		}, fileHeaders));
		body.add("messageData", objectMapper.writeValueAsString(Map.of("senderUsername", "alice", "roomId", firstRoomId())));

		var response = rest.postForEntity("/api/chat/upload", body, Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String downloadUrl = (String) ((Map<?, ?>) response.getBody().get("data")).get("downloadUrl");
		assertThat(rest.getForObject(downloadUrl, String.class)).isEqualTo("hello file");
	}

	// 下載時不用資料庫記錄的路徑，而是依序找「今天、昨天、前天」的日期目錄：3 天前上傳的檔案找不到
	@Test
	void fileFromThreeDaysAgo_shouldNotBeFound() throws Exception {
		var threeDaysAgo = UPLOAD_DIR.resolve(LocalDateTime.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
		Files.createDirectories(threeDaysAgo);
		Files.writeString(threeDaysAgo.resolve("old.txt"), "old");
		var twoDaysAgo = UPLOAD_DIR.resolve(LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
		Files.createDirectories(twoDaysAgo);
		Files.writeString(twoDaysAgo.resolve("recent.txt"), "recent");

		assertThat(rest.getForEntity("/api/files/download/old.txt", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(rest.getForObject("/api/files/download/recent.txt", String.class)).isEqualTo("recent");
	}

	// ScheduledTaskService 的 @Scheduled（每天清理舊訊息）沒有 @EnableScheduling：排程從未啟用
	@Test
	void scheduledCleanup_shouldNeverRun() {
		assertThat(context.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
	}
}
