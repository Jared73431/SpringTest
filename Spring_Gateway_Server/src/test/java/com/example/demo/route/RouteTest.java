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
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Baseline：用 WireMock 模擬後端 first-service，鎖定目前 Gateway 的路由行為。
 * 路由的 uri 寫死為 http://localhost:8081/，因此 WireMock 只能固定使用 8081。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RouteTest {

	@RegisterExtension
	static WireMockExtension backend = WireMockExtension.newInstance().options(wireMockConfig().port(8081)).build();

	@Autowired
	private WebTestClient webTestClient;

	// [Potential Bug] 後端實際路徑是 /employee/message，但 StripPrefix=1 轉送成 /message，經過 Gateway 永遠 404
	@Test
	void employeeRoute_shouldReturnNotFound_whenBackendServesEmployeeMessage() {
		backend.stubFor(get("/employee/message").willReturn(aResponse().withBody("Hello JavaInUse")));

		webTestClient.get().uri("/employee/message").exchange().expectStatus().isNotFound();
	}

	@Test
	void employeeRoute_shouldStripFirstPathSegment() {
		backend.stubFor(get("/message").willReturn(aResponse().withBody("stripped")));

		webTestClient.get().uri("/employee/message").exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("stripped");
		backend.verify(getRequestedFor(urlEqualTo("/message")));
	}

	@Test
	void unknownPath_shouldReturnNotFound() {
		webTestClient.get().uri("/unknown").exchange().expectStatus().isNotFound();
	}
}
