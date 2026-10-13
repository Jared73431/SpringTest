package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;

import com.example.demo.jakarta.EchoEndpoint;
import com.example.demo.spring.EchoHandler;

/**
 * 第 1 課：同一個 Echo，兩種寫法。
 *
 * <pre>
 *                Jakarta @ServerEndpoint          Spring WebSocketHandler
 * 是什麼         Java 標準 API（Tomcat 實作）      Spring 包裝的 API
 * 物件           每條連線建立一個新的              所有連線共用一個（Spring Bean）
 * 連線怎麼取得   存在欄位裡                        每個方法的參數傳進來
 * 註冊           ServerEndpointExporter            WebSocketConfigurer
 * Origin 檢查    預設不檢查                        預設只允許同源（第 3 課）
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L01_JakartaVsSpringTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext context;

	@ParameterizedTest
	@ValueSource(strings = { "/ws/jakarta/echo", "/ws/spring/echo" })
	void message_shouldBeAnsweredWithHello(String path) throws Exception {
		var client = TestClient.connect(port, path);

		client.send("Jared");

		assertThat(client.next()).matches("\\[\\d{13}\\] Hello Jared");
		client.close();
	}

	// 關閉連線時帶一個狀態碼：1000 正常關閉、1001 離開（例如關掉分頁）、1011 伺服器錯誤
	@ParameterizedTest
	@ValueSource(strings = { "/ws/jakarta/echo", "/ws/spring/echo" })
	void bye_shouldMakeTheServerCloseWith1000(String path) throws Exception {
		var client = TestClient.connect(port, path);

		client.send("bye");

		var status = client.closed.get(5, TimeUnit.SECONDS);
		assertThat(status.getCode()).isEqualTo(1000);
		assertThat(status.getReason()).isEqualTo("Bye");
	}

	@Test
	void jakartaEndpoint_shouldCreateANewObjectForEveryConnection() throws Exception {
		int before = EchoEndpoint.INSTANCES.get();

		var first = TestClient.connect(port, "/ws/jakarta/echo");
		var second = TestClient.connect(port, "/ws/jakarta/echo");

		assertThat(EchoEndpoint.INSTANCES.get()).isEqualTo(before + 2);
		first.close();
		second.close();
	}

	// Spring 的 handler 只有一個：連線不能存在欄位裡
	@Test
	void springHandler_shouldBeASingleObjectSharedByEveryConnection() {
		assertThat(context.getBeansOfType(EchoHandler.class)).hasSize(1);
	}
}
