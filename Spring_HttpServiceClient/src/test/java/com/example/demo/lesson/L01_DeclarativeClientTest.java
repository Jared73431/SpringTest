package com.example.demo.lesson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.example.demo.client.BookClient;
import com.example.demo.dto.BookResponse;

/**
 * 第 1 課：宣告式的 HTTP Client —— 只寫介面，Spring 產生實作。
 *
 * <pre>
 * bookClient.create(request)
 *   → @PostExchange("/books") + @HttpExchange("/api") → POST {base-url}/api/books，request 轉成 JSON
 *   → 回應的 JSON 轉成 BookResponse
 * </pre>
 *
 * 和 Spring_Feign_Client 的 API 完全相同，可以對照兩個模組的 BookClient。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L01_DeclarativeClientTest extends WireMockTestBase {

	@Autowired
	private BookClient bookClient;

	// 注入的是 Spring 在啟動時產生的實作（JDK 動態代理），我們沒有寫任何實作類別
	@Test
	void bookClient_shouldBeAnImplementationGeneratedBySpring() {
		assertThat(Proxy.isProxyClass(bookClient.getClass())).isTrue();
	}

	@Test
	void hello_shouldReturnServerHello() {
		server.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello")));

		assertThat(client().get().uri("/api/hello").retrieve().body(String.class)).isEqualTo("Hello");
	}

	// 回傳型別是 List<BookResponse>：JSON 直接轉成有型別的物件
	@Test
	void findAll_shouldReturnTypedBooks() {
		server.stubFor(get("/api/books").willReturn(okJson("[" + JAVA_BOOK + "]")));

		assertThat(bookClient.findAll())
				.containsExactly(new BookResponse(1, 12345, "Java", "Tom", 2020, "OReilly", 450.5));
	}

	// @RequestBody 的參數轉成 JSON 送出；本服務回 201，Location 指向本服務的網址
	@Test
	void create_shouldPostTheRequestAsJson() {
		server.stubFor(post("/api/books").willReturn(okJson(JAVA_BOOK)));

		var response = client().post().uri("/api/books").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("isbn", 12345, "title", "Java", "author", "Tom", "year", 2020, "publisher", "OReilly",
						"cost", 450.5))
				.retrieve().toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
				});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasToString("/api/books/1");
		server.verify(postRequestedFor(urlEqualTo("/api/books"))
				.withHeader("Content-Type", equalTo("application/json"))
				.withRequestBody(equalToJson("""
						{"isbn": 12345, "title": "Java", "author": "Tom", "year": 2020, "publisher": "OReilly", "cost": 450.5}
						""")));
	}

	@Test
	void delete_shouldSendDelete_andReturn204() {
		server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete("/api/books/1")
				.willReturn(aResponse().withStatus(204)));

		var response = client().delete().uri("/api/books/1").retrieve().toBodilessEntity();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		server.verify(deleteRequestedFor(urlEqualTo("/api/books/1")));
	}

	@Test
	void findAll_throughTheApi_shouldReturnTheBooks() {
		server.stubFor(get("/api/books").willReturn(okJson("[" + JAVA_BOOK + "]")));

		List<Map<String, Object>> books = client().get().uri("/api/books").retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		assertThat(books).extracting(book -> book.get("title")).containsExactly("Java");
	}
}
