package com.example.demo.lesson;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** 課程共用的 STOMP 客戶端：用名稱連線、訂閱、送訊息 */
final class ChatClient {

	final StompSession session;

	private ChatClient(StompSession session) {
		this.session = session;
	}

	static ChatClient connect(int port, String name) throws Exception {
		return connect(port, name, new WebSocketHttpHeaders());
	}

	static ChatClient connect(int port, String name, WebSocketHttpHeaders headers) throws Exception {
		var client = new WebSocketStompClient(new StandardWebSocketClient());
		// 伺服器回傳 String（錯誤訊息）時用 text/plain，其他用 JSON：兩種都要能轉換
		client.setMessageConverter(
				new CompositeMessageConverter(List.of(new StringMessageConverter(), new JacksonJsonMessageConverter())));
		var session = client.connectAsync("ws://localhost:" + port + "/ws?name=" + name, headers,
				new StompSessionHandlerAdapter() {
				}).get(5, TimeUnit.SECONDS);
		return new ChatClient(session);
	}

	/** 訂閱，回傳收到的訊息（JSON 物件轉成 Map、陣列轉成 List、純文字轉成 String） */
	BlockingQueue<Object> subscribe(String destination, Class<?> type) throws InterruptedException {
		BlockingQueue<Object> received = new LinkedBlockingQueue<>();
		session.subscribe(destination, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return type;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				received.add(payload);
			}
		});
		Thread.sleep(200); // 等 SUBSCRIBE 送達伺服器
		return received;
	}

	void send(String destination, Object payload) {
		session.send(destination, payload);
	}

	void join(String room) {
		session.send("/app/rooms/" + room + "/join", Map.of());
	}

	void disconnect() {
		session.disconnect();
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> next(BlockingQueue<Object> queue) throws InterruptedException {
		return (Map<String, Object>) queue.poll(5, TimeUnit.SECONDS);
	}
}
