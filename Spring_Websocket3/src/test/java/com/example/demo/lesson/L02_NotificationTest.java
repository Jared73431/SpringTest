package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.example.demo.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 2 課：上傳完成後，用 WebSocket 通知所有人。
 *
 * <pre>
 * 上傳者  ──HTTP POST /api/files──→ 伺服器 ──存檔、寫入資料庫
 *                                      └──STOMP /topic/files──→ 每個開著頁面的人
 * </pre>
 *
 * 伺服器可以在任何地方（這裡是 FileService）用 SimpMessagingTemplate 主動送訊息，不一定要由客戶端的訊息觸發。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L02_NotificationTest extends IntegrationTestBase {

	@Test
	@SuppressWarnings("unchecked")
	void upload_shouldNotifyEverySubscriber() throws Exception {
		var stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		stompClient.setMessageConverter(new JacksonJsonMessageConverter());
		var session = stompClient.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
		}).get(5, TimeUnit.SECONDS);
		BlockingQueue<Map<String, Object>> notifications = new LinkedBlockingQueue<>();
		session.subscribe("/topic/files", new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Map.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				notifications.add((Map<String, Object>) payload);
			}
		});
		Thread.sleep(200); // 等 SUBSCRIBE 送達

		String id = JsonPath.read(upload("news.txt", "new!").getBody(), "$.id");

		assertThat(notifications.poll(5, TimeUnit.SECONDS))
				.containsEntry("id", id)
				.containsEntry("name", "news.txt")
				.containsEntry("downloadUrl", "/api/files/" + id + "/content");
		session.disconnect();
	}
}
