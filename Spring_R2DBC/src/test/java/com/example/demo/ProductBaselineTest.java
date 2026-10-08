package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Baseline：鎖定重構前的行為（包含 [Potential Bug]），之後的修改都要對照這份測試說明行為變更。
 * 使用 Testcontainers，不會連到本機的資料庫。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProductBaselineTest extends PostgresContainerTestBase {

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private DatabaseClient databaseClient;

	@Autowired
	private ApplicationContext context;

	@BeforeEach
	void cleanTable() {
		databaseClient.sql("TRUNCATE product RESTART IDENTITY").then().block();
	}

	private ResponseEntity<String> save(String description, String price) {
		return rest.postForEntity("/SaveProduct?description={d}&price={p}", null, String.class, description, price);
	}

	// [Potential Bug] 使用 R2DBC，卻以 Spring MVC（Servlet）執行，Service 內用 block() 等待結果
	@Test
	void context_shouldBeServletBased() {
		assertThat(context).isInstanceOf(WebApplicationContext.class);
	}

	// POST 用 query 參數，成功時回傳 200 且沒有內容
	@Test
	void saveProduct_shouldReturn200WithoutBody() {
		ResponseEntity<String> response = save("Keyboard", "1200.5");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNull();
	}

	@Test
	void saveProduct_shouldReturn400_whenParameterIsMissing() {
		ResponseEntity<String> response = rest.postForEntity("/SaveProduct?description=Keyboard", null, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	// [Potential Bug] Persistable 的 isNew() 被 Jackson 當成 getter，回應多出 new、newProduct 兩個欄位
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void findAll_shouldReturnProductsIncludingPersistableFields() {
		save("Keyboard", "1200.5");
		save("Mouse", "350");

		ResponseEntity<List> response = rest.getForEntity("/findAll", List.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> products = response.getBody();
		assertThat(products).hasSize(2);
		assertThat(products.get(0)).containsOnlyKeys("id", "description", "price", "newProduct", "new");
		assertThat(products).extracting(p -> p.get("description")).containsExactlyInAnyOrder("Keyboard", "Mouse");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void getById_shouldReturnProduct_whenExists() {
		save("Keyboard", "1200.5");

		ResponseEntity<Map> response = rest.getForEntity("/getbyId/1", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("id", 1).containsEntry("description", "Keyboard")
				.containsEntry("price", 1200.5);
	}

	// [Potential Bug] 查不到時回傳 null：200 且沒有內容，不是 404
	@Test
	void getById_shouldReturn200WithoutBody_whenMissing() {
		ResponseEntity<String> response = rest.getForEntity("/getbyId/999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNull();
	}
}
