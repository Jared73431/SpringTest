package com.example.demo.lesson;

import static com.example.demo.lesson.ChatClient.next;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 2 課：誰在線上 —— 加入時加入名單，斷線時移除。
 *
 * <p>
 * 斷線不一定是使用者按「離開」：關掉分頁、網路中斷都算。Spring 在連線關閉時發出 SessionDisconnectEvent，
 * PresenceEventListener 在這裡移除。修正前的 Spring_Websocket2 斷線時沒有移除（baseline 確認），名單只增不減。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L02_PresenceTest extends PostgresContainerTestBase {

	@LocalServerPort
	private int port;

	@Test
	void join_shouldBroadcastAJoinMessageAndTheUserList() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var room = alice.subscribe("/topic/rooms/join-test", Map.class);
		var users = alice.subscribe("/topic/rooms/join-test/users", List.class);

		alice.join("join-test");

		assertThat(next(room)).containsEntry("type", "JOIN").containsEntry("content", "alice joined");
		assertThat(users.poll(5, TimeUnit.SECONDS)).isEqualTo(List.of("alice"));
		alice.disconnect();
	}

	@Test
	void disconnect_shouldBroadcastLeaveAndTheNewUserList() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var room = alice.subscribe("/topic/rooms/leave-test", Map.class);
		var users = alice.subscribe("/topic/rooms/leave-test/users", List.class);
		alice.join("leave-test");
		next(room);
		users.poll(5, TimeUnit.SECONDS);
		var bob = ChatClient.connect(port, "bob");
		bob.join("leave-test");
		next(room);
		assertThat(users.poll(5, TimeUnit.SECONDS)).isEqualTo(List.of("alice", "bob"));

		bob.disconnect(); // 斷線，沒有送任何「離開」的訊息

		assertThat(next(room)).containsEntry("type", "LEAVE").containsEntry("content", "bob left");
		assertThat(users.poll(5, TimeUnit.SECONDS)).isEqualTo(List.of("alice"));
		alice.disconnect();
	}
}
