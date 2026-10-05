package com.example.demo.route;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Gateway 路由測試：以隨機 port 啟動 Gateway，用 WireMock 模擬後端，
 * 驗證「哪些路徑會被轉送、轉送到哪裡、路徑如何改寫、回應加上了什麼」。
 *
 * <ul>
 * <li>first-service：一個 WireMock，位址由 FIRST_SERVICE_URL 指定</li>
 * <li>service-provider：兩個 WireMock 代表兩台 Provider；測試不啟動 Eureka，
 * 改用 SimpleDiscoveryClient 在設定中列出實例，lb:// 仍會經過 LoadBalancer</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ExtendWith(OutputCaptureExtension.class)
class RouteTest {

	@RegisterExtension
	static WireMockExtension firstService = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort())
			.build();

	@RegisterExtension
	static WireMockExtension providerA = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort())
			.build();

	@RegisterExtension
	static WireMockExtension providerB = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void backends(DynamicPropertyRegistry registry) {
		// 對應 application.yml 的 uri: ${FIRST_SERVICE_URL:...}
		registry.add("FIRST_SERVICE_URL", firstService::baseUrl);
		registry.add("eureka.client.enabled", () -> "false");
		registry.add("spring.cloud.discovery.client.simple.instances.service-provider[0].uri", providerA::baseUrl);
		registry.add("spring.cloud.discovery.client.simple.instances.service-provider[1].uri", providerB::baseUrl);
	}

	@Autowired
	private WebTestClient webTestClient;

	// ===== first-service：固定網址 + StripPrefix =====

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

	// ===== books-service：lb:// + RewritePath =====

	@Test
	void booksServiceRoute_shouldRewritePathAndForwardToProvider() {
		providerA.stubFor(get("/api/books/1").willReturn(aResponse().withBody("book 1")));
		providerB.stubFor(get("/api/books/1").willReturn(aResponse().withBody("book 1")));

		webTestClient.get().uri("/books-service/api/books/1").exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("book 1");
		int received = providerA.findAll(getRequestedFor(urlEqualTo("/api/books/1"))).size()
				+ providerB.findAll(getRequestedFor(urlEqualTo("/api/books/1"))).size();
		assertThat(received).isEqualTo(1);
	}

	// 同一個服務有兩個實例時，LoadBalancer（預設 Round Robin）把請求輪流送到兩台
	@Test
	void booksServiceRoute_shouldAlternateBetweenInstances() {
		providerA.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello from A")));
		providerB.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello from B")));

		List<String> responses = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			responses.add(webTestClient.get().uri("/books-service/api/hello").exchange()
					.expectStatus().isOk()
					.expectBody(String.class).returnResult().getResponseBody());
		}

		// Round Robin 的起點是隨機的，所以只檢查「相鄰兩次一定不同台」與兩台各處理一半
		assertThat(responses.get(0)).isNotEqualTo(responses.get(1));
		assertThat(responses).filteredOn("Hello from A"::equals).hasSize(2);
		assertThat(responses).filteredOn("Hello from B"::equals).hasSize(2);
	}

	// ===== filters =====

	// default-filters 的 AddResponseHeader 套用到所有路由
	@Test
	void allRoutes_shouldAddGatewayResponseHeader() {
		firstService.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello")));
		providerA.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello")));
		providerB.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello")));

		webTestClient.get().uri("/first-service/api/hello").exchange()
				.expectHeader().valueEquals("X-Gateway", "api-gateway");
		webTestClient.get().uri("/books-service/api/hello").exchange()
				.expectHeader().valueEquals("X-Gateway", "api-gateway");
	}

	// RequestLoggingFilter：記錄原始路徑、實際轉送的網址、狀態碼
	@Test
	void requestLoggingFilter_shouldLogRoutedRequest(CapturedOutput output) {
		firstService.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello")));

		webTestClient.get().uri("/first-service/api/hello").exchange().expectStatus().isOk();

		assertThat(output).contains("GET /first-service/api/hello -> " + firstService.baseUrl() + "/api/hello 200 OK");
	}

	// ===== 不符合任何路由 =====

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
