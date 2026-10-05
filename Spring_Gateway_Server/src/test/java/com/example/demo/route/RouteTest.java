package com.example.demo.route;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Gateway 路由測試：以隨機 port 啟動 Gateway，用 WireMock 模擬後端 first-service，
 * 驗證「哪些路徑會被轉送、轉送到後端的路徑是什麼」。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RouteTest {

	@RegisterExtension
	static WireMockExtension firstService = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort())
			.build();

	// 對應 application.yml 的 uri: ${FIRST_SERVICE_URL:...}
	@DynamicPropertySource
	static void backendUrl(DynamicPropertyRegistry registry) {
		registry.add("FIRST_SERVICE_URL", firstService::baseUrl);
	}

	@Autowired
	private WebTestClient webTestClient;

	// 修正前：StripPrefix=1 把 /employee/message 轉成 /message，但後端路徑是 /employee/message，經過 Gateway 永遠 404
	@Test
	void firstServiceRoute_shouldStripPrefixAndForwardToApiPath() {
		firstService.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello from first-service")));

		webTestClient.get().uri("/first-service/api/hello").exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello from first-service");
		firstService.verify(getRequestedFor(urlEqualTo("/api/hello")));
	}

	// 後端回傳的狀態碼原樣轉回
	@Test
	void firstServiceRoute_shouldPassThroughBackendStatus() {
		firstService.stubFor(get("/api/missing").willReturn(aResponse().withStatus(404)));

		webTestClient.get().uri("/first-service/api/missing").exchange().expectStatus().isNotFound();
	}

	@Test
	void unknownPath_shouldReturnNotFound() {
		webTestClient.get().uri("/unknown").exchange().expectStatus().isNotFound();
	}

	// 舊路由（/employee/**、/consumer/**）已移除
	@Test
	void legacyRoutes_shouldReturnNotFound() {
		webTestClient.get().uri("/employee/message").exchange().expectStatus().isNotFound();
		webTestClient.get().uri("/consumer/anything").exchange().expectStatus().isNotFound();
	}
}
