package com.example.demo.lesson;

import static com.example.demo.lesson.ChatClient.next;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 3 課：私訊 —— 只送給某一個人。
 *
 * <pre>
 * 每個人都訂閱 /user/queue/private（不寫自己的名字）
 * 伺服器 convertAndSendToUser("bob", "/queue/private", 訊息)
 *   → Spring 依「名稱」找到 bob 的連線（SimpUserRegistry）→ 只有 bob 收到
 * </pre>
 *
 * 「名稱」是連線的 Principal。修正前的 Spring_Websocket3 沒有 Principal，所以私訊永遠送不到（baseline 確認）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L03_PrivateMessageTest extends PostgresContainerTestBase {

	@LocalServerPort
	private int port;

	@Test
	void privateMessage_shouldReachOnlyTheRecipient_andTheSender() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var bob = ChatClient.connect(port, "bob");
		var carol = ChatClient.connect(port, "carol");
		var alicePrivate = alice.subscribe("/user/queue/private", Map.class);
		var bobPrivate = bob.subscribe("/user/queue/private", Map.class);
		var carolPrivate = carol.subscribe("/user/queue/private", Map.class);

		alice.send("/app/private", Map.of("to", "bob", "content", "secret"));

		assertThat(next(bobPrivate)).containsEntry("from", "alice").containsEntry("to", "bob")
				.containsEntry("content", "secret");
		assertThat(next(alicePrivate)).containsEntry("content", "secret"); // 自己的畫面也要顯示
		assertThat(carolPrivate.poll(1, TimeUnit.SECONDS)).isNull();
		alice.disconnect();
		bob.disconnect();
		carol.disconnect();
	}

	// 對方不在線上：告訴發送者，而不是默默丟掉
	@Test
	void privateMessage_toAnOfflineUser_shouldReturnAnError() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var errors = alice.subscribe("/user/queue/errors", String.class);

		alice.send("/app/private", Map.of("to", "dave", "content", "are you there?"));

		assertThat(errors.poll(5, TimeUnit.SECONDS)).isEqualTo("User is offline: dave");
		alice.disconnect();
	}
}
