package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Baseline：鎖定重構前的行為（包含 [Potential Bug]），之後的修改都要對照這份測試說明行為變更。
 * 每個測試前重設成 Flyway V1 放入的 5 筆範例資料。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CarBaselineTest extends PostgresContainerTestBase {

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

	// [Potential Bug] 驗證允許 1900，資料庫 CHECK 是 year > 1900；錯誤變成 500，而且回傳資料庫的錯誤訊息
	@Test
	void create_shouldReturn500WithDatabaseMessage_whenYearIs1900() {
		webTestClient.post().uri("/api/cars")
				.bodyValue("""
						{"make":"Ford","model":"T","year":1900,"price":850}
						""")
				.header("Content-Type", "application/json")
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody()
				.jsonPath("$.message").isEqualTo("Internal server error")
				.jsonPath("$.error").value(error -> assertThat(error.toString()).contains("car_year_check"));
	}

	// 驗證錯誤：自訂的 Map 格式（不是 ProblemDetail）
	@Test
	void create_shouldReturn400WithFieldErrors_whenInvalid() {
		webTestClient.post().uri("/api/cars")
				.bodyValue("""
						{"make":"","model":"Camry","year":2022,"price":1}
						""")
				.header("Content-Type", "application/json")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.message").isEqualTo("Validation failed")
				.jsonPath("$.errors.make").isEqualTo("Make is required");
	}

	// [Potential Bug] id 不是數字：catch-all 的 @ExceptionHandler(Exception.class) 把 400 變成 500
	@Test
	void findById_shouldReturn500_whenIdIsNotNumeric() {
		webTestClient.get().uri("/api/cars/abc").exchange()
				.expectStatus().is5xxServerError()
				.expectBody().jsonPath("$.message").isEqualTo("Internal server error");
	}

	// [Potential Bug] 缺少必填的 make：同樣被變成 500
	@Test
	void count_shouldReturn500_whenMakeIsMissing() {
		webTestClient.get().uri("/api/cars/count").exchange()
				.expectStatus().is5xxServerError();
	}

	@Test
	void findById_shouldReturn404WithoutBody_whenMissing() {
		webTestClient.get().uri("/api/cars/999").exchange()
				.expectStatus().isNotFound()
				.expectBody().isEmpty();
	}

	// 刪除成功回傳 200（慣例是 204）
	@Test
	void delete_shouldReturn200() {
		webTestClient.delete().uri("/api/cars/1").exchange().expectStatus().isOk();
		webTestClient.get().uri("/api/cars/1").exchange().expectStatus().isNotFound();
	}
}
