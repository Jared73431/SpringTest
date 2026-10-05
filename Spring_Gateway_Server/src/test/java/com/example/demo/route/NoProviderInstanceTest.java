package com.example.demo.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * lb:// 路由找不到任何實例時（例如 Provider 全部停止，或還沒註冊到 Eureka），Gateway 回 503。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "eureka.client.enabled=false")
@AutoConfigureWebTestClient
class NoProviderInstanceTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void booksServiceRoute_shouldReturnServiceUnavailable_whenNoInstance() {
		webTestClient.get().uri("/books-service/api/books").exchange().expectStatus().isEqualTo(503);
	}
}
