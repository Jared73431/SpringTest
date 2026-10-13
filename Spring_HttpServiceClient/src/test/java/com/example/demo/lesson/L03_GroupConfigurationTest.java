package com.example.demo.lesson;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.client.BookClient;

/**
 * 第 3 課：設定集中在 application.properties。
 *
 * <pre>
 * @ImportHttpServices(group = "book-server", types = BookClient.class)
 *
 * spring.http.serviceclient.book-server.base-url=...         位址
 * spring.http.serviceclient.book-server.connect-timeout=2s   連線逾時
 * spring.http.serviceclient.book-server.read-timeout=5s      讀取逾時（第 2 課在測試中改成 500ms）
 * spring.http.serviceclient.book-server.default-header.X-Client-Name=...   每個請求都帶的標頭
 * </pre>
 *
 * 這些設定套用在整個群組：同一個服務有多個介面時，放在同一個群組就共用位址與設定。
 * Feign 的對應設定是 spring.cloud.openfeign.client.config.book-server.*。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L03_GroupConfigurationTest extends WireMockTestBase {

	@Autowired
	private BookClient bookClient;

	// base-url 由 WireMockTestBase 改成 WireMock 的位址；請求確實送到那裡，而且帶著 default-header
	@Test
	void everyRequest_shouldGoToTheConfiguredBaseUrl_withTheDefaultHeader() {
		server.stubFor(get("/api/books").willReturn(okJson("[]")));

		bookClient.findAll();

		server.verify(getRequestedFor(urlEqualTo("/api/books"))
				.withHeader("X-Client-Name", equalTo("spring-http-service-client")));
	}
}
