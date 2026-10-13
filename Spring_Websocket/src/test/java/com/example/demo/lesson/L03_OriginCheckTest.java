package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketHttpHeaders;

/**
 * 第 3 課：Origin 檢查 —— 哪些網站的頁面可以連進來？
 *
 * <p>
 * WebSocket 不受瀏覽器的同源政策限制：任何網站的頁面都能對你的伺服器開 WebSocket，而且瀏覽器會<b>自動帶上你的 cookie</b>。
 * 伺服器如果靠 cookie 認人，惡意網站就能冒用使用者的身分連線（Cross-Site WebSocket Hijacking，和 CSRF 同一類問題）。
 *
 * <p>
 * 防禦：檢查握手請求的 Origin 標頭（瀏覽器自動帶上、頁面無法偽造）。
 * <ul>
 * <li>Spring：預設只允許同源；要開放其他網站用 setAllowedOrigins("https://...")，不要用 "*"</li>
 * <li>Jakarta：預設不檢查，要自己寫 Configurator</li>
 * </ul>
 * 非瀏覽器的客戶端（curl、Postman、手機 App）沒有 Origin 標頭，不受影響。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L03_OriginCheckTest {

	@LocalServerPort
	private int port;

	private static WebSocketHttpHeaders origin(String origin) {
		var headers = new WebSocketHttpHeaders();
		headers.add(HttpHeaders.ORIGIN, origin);
		return headers;
	}

	@Test
	void springHandler_shouldRejectOtherSites() {
		assertThatThrownBy(() -> TestClient.connect(port, "/ws/spring/echo", origin("http://evil.example")))
				.hasMessageContaining("403");
	}

	@Test
	void springHandler_shouldAcceptTheSameOrigin() throws Exception {
		var client = TestClient.connect(port, "/ws/spring/echo", origin("http://localhost:" + port));

		assertThat(client.session.isOpen()).isTrue();
		client.close();
	}

	@Test
	void springHandler_shouldAcceptClientsWithoutOrigin() throws Exception {
		var client = TestClient.connect(port, "/ws/spring/echo");

		assertThat(client.session.isOpen()).isTrue();
		client.close();
	}

	// [Learning] Jakarta 的 endpoint 預設不檢查 Origin
	@Test
	void jakartaEndpoint_shouldAcceptAnySite() throws Exception {
		var client = TestClient.connect(port, "/ws/jakarta/echo", origin("http://evil.example"));

		assertThat(client.session.isOpen()).isTrue();
		client.close();
	}
}
