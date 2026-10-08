package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;

/**
 * /api/products 的 API 測試（取代修正前的 baseline：/findAll、/SaveProduct、/getbyId/{Id}）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProductApiTest extends PostgresContainerTestBase {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private DatabaseClient databaseClient;

	@BeforeEach
	void cleanTable() {
		databaseClient.sql("TRUNCATE product RESTART IDENTITY CASCADE").then().block();
	}

	private ProductResponse create(String description, String price) {
		return webTestClient.post().uri("/api/products")
				.bodyValue(new ProductRequest(description, new BigDecimal(price)))
				.exchange()
				.expectStatus().isCreated()
				.expectBody(ProductResponse.class).returnResult().getResponseBody();
	}

	// 修正前：POST /SaveProduct?description=&price=，回傳 200 且沒有內容
	@Test
	void create_shouldReturn201WithLocationAndBody() {
		webTestClient.post().uri("/api/products")
				.bodyValue(new ProductRequest("Keyboard", new BigDecimal("1200.50")))
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().location("/api/products/1")
				.expectBody(ProductResponse.class)
				.isEqualTo(new ProductResponse(1, "Keyboard", new BigDecimal("1200.50")));
	}

	@Test
	void create_shouldReturn400ProblemDetail_whenRequestIsInvalid() {
		webTestClient.post().uri("/api/products")
				.bodyValue(new ProductRequest(" ", new BigDecimal("-1")))
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// 修正前：回應多出 Persistable 的 new、newProduct 欄位
	@Test
	void findAll_shouldReturnOnlyProductFields() {
		create("Keyboard", "1200.50");
		create("Mouse", "350");

		webTestClient.get().uri("/api/products").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].new").doesNotExist()
				.jsonPath("$[0].newProduct").doesNotExist()
				.jsonPath("$[*].description").value(descriptions -> assertThat(descriptions.toString())
						.contains("Keyboard", "Mouse"));
	}

	@Test
	void findById_shouldReturnProduct_whenExists() {
		ProductResponse created = create("Keyboard", "1200.50");

		webTestClient.get().uri("/api/products/{id}", created.id()).exchange()
				.expectStatus().isOk()
				.expectBody(ProductResponse.class).isEqualTo(created);
	}

	// 修正前：查不到時回傳 200 且沒有內容
	@Test
	void findById_shouldReturn404ProblemDetail_whenMissing() {
		webTestClient.get().uri("/api/products/999").exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody().jsonPath("$.detail").isEqualTo("找不到商品：999");
	}

	@Test
	void update_shouldChangeProduct() {
		ProductResponse created = create("Keyboard", "1200.50");

		webTestClient.put().uri("/api/products/{id}", created.id())
				.bodyValue(new ProductRequest("Mechanical Keyboard", new BigDecimal("2500")))
				.exchange()
				.expectStatus().isOk()
				.expectBody(ProductResponse.class)
				.isEqualTo(new ProductResponse(created.id(), "Mechanical Keyboard", new BigDecimal("2500")));
	}

	@Test
	void update_shouldReturn404_whenMissing() {
		webTestClient.put().uri("/api/products/999")
				.bodyValue(new ProductRequest("Keyboard", BigDecimal.ONE))
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void delete_shouldReturn204AndRemoveProduct() {
		ProductResponse created = create("Keyboard", "1200.50");

		webTestClient.delete().uri("/api/products/{id}", created.id()).exchange().expectStatus().isNoContent();
		webTestClient.get().uri("/api/products/{id}", created.id()).exchange().expectStatus().isNotFound();
	}

	@Test
	void delete_shouldReturn404_whenMissing() {
		webTestClient.delete().uri("/api/products/999").exchange().expectStatus().isNotFound();
	}

	// 金額使用 NUMERIC + BigDecimal，不會有浮點數誤差
	@Test
	void price_shouldKeepExactDecimal() {
		ProductResponse created = create("Cable", "0.30");

		assertThat(created.price()).isEqualByComparingTo("0.30");
		assertThat(new BigDecimal("0.1").add(new BigDecimal("0.2"))).isEqualByComparingTo(created.price());
	}
}
