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

	// [Potential Bug] 搜尋只看第一個有值的參數：make 和 year 同時給，year 被忽略
	@Test
	void search_shouldIgnoreYear_whenMakeIsAlsoGiven() {
		webTestClient.get().uri("/api/cars/search?make=Toyota&year=2021").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(1)
				.jsonPath("$[0].year").isEqualTo(2022);
	}

	// [Potential Bug] 只給 minPrice、沒有 maxPrice 時，條件被忽略，回傳全部
	@Test
	void search_shouldReturnAll_whenOnlyMinPriceIsGiven() {
		webTestClient.get().uri("/api/cars/search?minPrice=40000").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(5);
	}

	// [Potential Bug] 搜尋用 ILIKE（不分大小寫），計數用 =（分大小寫）
	@Test
	void searchAndCount_shouldDisagreeOnCase() {
		webTestClient.get().uri("/api/cars/search?make=toyota").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(1);
		webTestClient.get().uri("/api/cars/count?make=toyota").exchange()
				.expectStatus().isOk()
				.expectBody(Long.class).isEqualTo(0L);
	}

	// [Potential Bug] 驗證允許 1900，資料庫 CHECK 是 year > 1900。
	// 修正前：500，回應中帶有資料庫的 constraint 名稱（car_year_check）；現在：409，不帶資料庫的訊息
	@Test
	void create_shouldReturn409WithoutDatabaseMessage_whenYearIs1900() {
		webTestClient.post().uri("/api/cars")
				.bodyValue("""
						{"make":"Ford","model":"T","year":1900,"price":850}
						""")
				.header("Content-Type", "application/json")
				.exchange()
				.expectStatus().isEqualTo(409)
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.detail").isEqualTo("資料違反資料庫的限制條件")
				.consumeWith(result -> assertThat(new String(result.getResponseBodyContent()))
						.doesNotContain("car_year_check"));
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
