package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * /api/cars 的 API 測試（由 baseline 演變而來：修正一個問題，就把對應的測試改成新的行為，註解保留修正前的行為）。
 * 每個測試前重設成 Flyway V1 放入的 5 筆範例資料。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CarApiTest extends PostgresContainerTestBase {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private DatabaseClient databaseClient;

	@BeforeEach
	void resetSampleData() {
		databaseClient.sql("TRUNCATE car RESTART IDENTITY").then()
				.then(databaseClient.sql("""
						INSERT INTO car (make, model, year, color, price) VALUES
						    ('Toyota', 'Camry', 2022, 'White', 25000.00),
						    ('Honda', 'Civic', 2021, 'Blue', 22000.00),
						    ('BMW', 'X3', 2023, 'Black', 45000.00),
						    ('Mercedes', 'C-Class', 2022, 'Silver', 42000.00),
						    ('Audi', 'A4', 2021, 'Red', 38000.00)
						""").then())
				.block();
	}

	@Test
	void findAll_shouldReturnSampleData() {
		webTestClient.get().uri("/api/cars").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(5);
	}

	// 修正前：只看第一個有值的參數，make=Toyota&year=2021 會忽略 year，回傳 2022 年的 Camry
	@Test
	void search_shouldCombineMakeAndYear() {
		webTestClient.get().uri("/api/cars/search?make=Toyota&year=2021").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(0);
	}

	// 修正前：只給 minPrice、沒有 maxPrice 時，條件被忽略，回傳全部 5 台
	@Test
	void search_shouldUseMinPriceAlone() {
		webTestClient.get().uri("/api/cars/search?minPrice=40000").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$[*].make").value(makes -> assertThat(makes.toString())
						.contains("BMW", "Mercedes").doesNotContain("Toyota", "Audi"));
	}

	@Test
	void search_shouldCombineYearRangeAndMaxPrice() {
		webTestClient.get().uri("/api/cars/search?yearFrom=2022&maxPrice=43000").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].make").isEqualTo("Toyota")
				.jsonPath("$[1].make").isEqualTo("Mercedes");
	}

	@Test
	void search_shouldReturnAll_whenNoConditionIsGiven() {
		webTestClient.get().uri("/api/cars/search").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(5);
	}

	// 修正前：搜尋用 ILIKE（不分大小寫）找到 1 台，計數用 =（分大小寫）卻是 0
	@Test
	void searchAndCount_shouldAgreeIgnoringCase() {
		webTestClient.get().uri("/api/cars/search?make=toyota").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(1);
		webTestClient.get().uri("/api/cars/count?make=toyota").exchange()
				.expectStatus().isOk()
				.expectBody(Long.class).isEqualTo(1L);
	}

	// 修正前：驗證是 @Min(1900)，資料庫的 CHECK 是 year > 1900，1900 通過驗證後寫入失敗，回傳 500 並帶出 constraint 名稱。
	// 現在：驗證改成 @Min(1901)，回傳 400
	@Test
	void create_shouldReturn400_whenYearIs1900() {
		webTestClient.post().uri("/api/cars")
				.bodyValue("""
						{"make":"Ford","model":"T","year":1900,"price":850}
						""")
				.header("Content-Type", "application/json")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.year").isEqualTo("Year must be greater than 1900");
	}

	// [Potential Bug] 修正前沒有長度驗證：超過 VARCHAR(50) 的值寫入時才失敗，回傳 500
	// （PostgreSQL 的 R2DBC 驅動把「value too long」歸類為 BadGrammar，不是 DataIntegrityViolation）
	@Test
	void update_shouldReturn400_whenModelIsTooLong() {
		webTestClient.put().uri("/api/cars/1")
				.bodyValue("""
						{"make":"Toyota","model":"%s","year":2022,"price":1}
						""".formatted("x".repeat(51)))
				.header("Content-Type", "application/json")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.model").isEqualTo("Model must be at most 50 characters");
	}

	// 修正前：自訂的 Map 格式（message、errors）；現在：ProblemDetail，保留每個欄位的錯誤訊息
	@Test
	void create_shouldReturn400ProblemDetailWithFieldErrors_whenInvalid() {
		webTestClient.post().uri("/api/cars")
				.bodyValue("""
						{"make":"","model":"Camry","year":2022,"price":1}
						""")
				.header("Content-Type", "application/json")
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(400)
				.jsonPath("$.errors.make").isEqualTo("Make is required");
	}

	// 修正前：catch-all 的 @ExceptionHandler(Exception.class) 把它變成 500
	@Test
	void findById_shouldReturn400_whenIdIsNotNumeric() {
		webTestClient.get().uri("/api/cars/abc").exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// 修正前：同樣被變成 500
	@Test
	void count_shouldReturn400_whenMakeIsMissing() {
		webTestClient.get().uri("/api/cars/count").exchange()
				.expectStatus().isBadRequest();
	}

	// 修正前：404 但沒有內容
	@Test
	void findById_shouldReturn404ProblemDetail_whenMissing() {
		webTestClient.get().uri("/api/cars/999").exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody().jsonPath("$.detail").isEqualTo("找不到汽車：999");
	}

	// 修正前：200
	@Test
	void delete_shouldReturn204() {
		webTestClient.delete().uri("/api/cars/1").exchange().expectStatus().isNoContent();
		webTestClient.get().uri("/api/cars/1").exchange().expectStatus().isNotFound();
	}

	@Test
	void delete_shouldReturn404_whenMissing() {
		webTestClient.delete().uri("/api/cars/999").exchange().expectStatus().isNotFound();
	}
}
