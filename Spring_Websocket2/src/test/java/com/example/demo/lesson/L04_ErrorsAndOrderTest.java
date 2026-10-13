package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketHttpHeaders;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 4 課：錯誤、順序與 Origin。
 *
 * <ul>
 * <li>錯誤送回發送者（@MessageExceptionHandler + @SendToUser）：修正前的 Spring_Websocket4 只寫 log，發送者不知道訊息沒送出</li>
 * <li>順序：客戶端送來的訊息預設由執行緒池平行處理，Spring 不保證順序。修正前的 baseline 遇過一次「加入」和「發言」
 * 顛倒（兩個處理方法花的時間不同）；這裡的 50 則發言處理得很快，關掉設定跑了 3 次都沒有顛倒，所以不容易重現，
 * 但不代表不會發生。setPreserveReceiveOrder(true) 讓同一個連線的訊息依序處理</li>
 * <li>Origin：沒有 setAllowedOriginPatterns("*")，其他網站的頁面不能連進來</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L04_ErrorsAndOrderTest extends PostgresContainerTestBase {

	@LocalServerPort
	private int port;

	@Test
	void invalidMessages_shouldBeReportedToTheSender() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var errors = alice.subscribe("/user/queue/errors", String.class);

		alice.send("/app/rooms/java/messages", Map.of("content", "   "));
		alice.send("/app/rooms/java/messages", Map.of("content", "x".repeat(501)));
		alice.send("/app/rooms/Not_Valid!/messages", Map.of("content", "hi"));

		assertThat(errors.poll(5, TimeUnit.SECONDS)).isEqualTo("Message must not be blank");
		assertThat(errors.poll(5, TimeUnit.SECONDS)).isEqualTo("Message is longer than 500 characters");
		assertThat(errors.poll(5, TimeUnit.SECONDS)).isEqualTo("Invalid room: Not_Valid!");
		alice.disconnect();
	}

	// 錯誤只送給發生錯誤的人
	@Test
	void errors_shouldNotReachOtherUsers() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var bob = ChatClient.connect(port, "bob");
		var bobErrors = bob.subscribe("/user/queue/errors", String.class);

		alice.send("/app/rooms/java/messages", Map.of("content", ""));

		assertThat(bobErrors.poll(1, TimeUnit.SECONDS)).isNull();
		alice.disconnect();
		bob.disconnect();
	}

	@Test
	void messagesSentBackToBack_shouldArriveInTheSameOrder() throws Exception {
		var alice = ChatClient.connect(port, "alice");
		var room = alice.subscribe("/topic/rooms/order", Map.class);

		for (int i = 0; i < 50; i++) {
			alice.send("/app/rooms/order/messages", Map.of("content", String.valueOf(i)));
		}

		List<Object> contents = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			contents.add(ChatClient.next(room).get("content"));
		}
		assertThat(contents).containsExactlyElementsOf(
				java.util.stream.IntStream.range(0, 50).mapToObj(String::valueOf).toList());
		alice.disconnect();
	}

	@Test
	void connection_shouldBeRejected_fromOtherSites() {
		var headers = new WebSocketHttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://evil.example");

		assertThatThrownBy(() -> ChatClient.connect(port, "mallory", headers)).hasMessageContaining("403");
	}
}
