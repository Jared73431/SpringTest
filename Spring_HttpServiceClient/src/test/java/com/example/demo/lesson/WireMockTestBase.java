package com.example.demo.lesson;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * 課程共用：用 WireMock 模擬 Spring_Feign_Server（不需要真正的 Server 與資料庫），並提供呼叫本服務的 RestClient。
 *
 * <p>
 * 整個測試期間只啟動一個 WireMock（位址固定）：Spring 會重用相同設定的測試 context，如果每個測試類別各自啟動
 * WireMock、port 每次不同，重用的 context 還記著舊的位址。
 *
 * <p>
 * http2PlainDisabled：Boot 4 的 RestClient 預設使用 JDK 的 HttpClient，會嘗試把明文連線升級成 HTTP/2（h2c）。
 * WireMock（Jetty）接受升級，但帶 body 的 POST 會失敗（RST_STREAM）；真正的 Server（Tomcat）不支援 h2c，
 * 會退回 HTTP/1.1，所以只有測試需要關掉。
 */
abstract class WireMockTestBase {

	static final WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort().http2PlainDisabled(true));

	static {
		server.start();
	}

	// 把群組 book-server 的位址改成 WireMock
	@DynamicPropertySource
	static void bookServerUrl(DynamicPropertyRegistry registry) {
		registry.add("spring.http.serviceclient.book-server.base-url", server::baseUrl);
	}

	@BeforeEach
	void resetWireMock() {
		server.resetAll();
	}

	static final String JAVA_BOOK = """
			{"id": 1, "isbn": 12345, "title": "Java", "author": "Tom", "year": 2020, "publisher": "OReilly", "cost": 450.5}
			""";

	static final String NOT_FOUND_PROBLEM = """
			{"type": "about:blank", "title": "Not Found", "status": 404, "detail": "Book not found: id=999"}
			""";

	@LocalServerPort
	int port;

	/** 呼叫本服務（Client）的 API；4xx、5xx 不拋例外，直接回傳給測試檢查 */
	RestClient client() {
		return RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.defaultStatusHandler(status -> true, (request, response) -> {
				})
				.build();
	}
}
