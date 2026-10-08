package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

/**
 * Baseline：鎖定重構前的行為（包含 [Potential Bug]），之後的修改都要對照這份測試說明行為變更。
 * 使用 Testcontainers，不會連到本機的資料庫。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@SuppressWarnings({ "rawtypes", "unchecked" })
class UserBaselineTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Environment environment;

	private static final Map<String, Object> JOHN = Map.of("username", "john_doe", "name", "John Doe", "email",
			"john@example.com", "age", 25);

	@BeforeEach
	void cleanTable() {
		userRepository.deleteAll();
	}

	private User saveDirectly() {
		return userRepository.save(User.builder().username("jane").name("Jane").email("jane@example.com").age(30).build());
	}

	// [Potential Bug] 問題疊在一起：
	// 1. @RequestBody 是 Swagger 的註解（io.swagger.v3.oas.annotations.parameters.RequestBody），Spring 不讀 JSON body
	// 2. User(username, name, email, age) 建構子是空的
	// 3. 驗證錯誤的明細（errors）沒有放進回應
	//
	// Boot 3.2 + springdoc 2.2.0 時專案沒有 Bean Validation 實作，@Valid 沒有作用，結果是回傳 200 並存入一筆全部是 null 的資料
	// （PUT 會把既有資料清成 null）。升級到 springdoc 2.8 後，它間接帶入 spring-boot-starter-validation，驗證開始生效，行為變成下面這樣
	@Test
	void create_shouldReturn400WithoutFieldErrors_whenSendingJson() {
		ResponseEntity<Map> response = rest.postForEntity("/api/users", JOHN, Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).containsEntry("code", 400).containsEntry("message", "參數驗證失敗")
				.containsEntry("data", null);
		assertThat(userRepository.count()).isZero();
	}

	// 改用 query 參數雖然綁定得到、也通過驗證，但空的建構子丟掉所有欄位，寫入前 Entity 的驗證失敗 → catch-all 回傳 500
	@Test
	void create_shouldReturn500_whenSendingQueryParameters() {
		ResponseEntity<Map> response = rest.postForEntity(
				"/api/users?username=john_doe&name=John&email=john@example.com&age=25", null, Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).containsEntry("message", "系統內部錯誤");
		assertThat(userRepository.count()).isZero();
	}

	// [Potential Bug] PUT 同樣讀不到 JSON body
	@Test
	void update_shouldReturn400_whenSendingJson() {
		User jane = saveDirectly();

		ResponseEntity<Map> response = rest.exchange("/api/users/" + jane.getId(), HttpMethod.PUT,
				new HttpEntity<>(JOHN), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(userRepository.findById(jane.getId()).orElseThrow().getUsername()).isEqualTo("jane");
	}

	@Test
	void findAll_shouldWrapUsersInApiResponse() {
		saveDirectly();

		ResponseEntity<Map> response = rest.getForEntity("/api/users", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("code", 200).containsEntry("message", "獲取用戶列表成功");
		assertThat((java.util.List) response.getBody().get("data")).hasSize(1);
	}

	@Test
	void findById_shouldWrapUserInApiResponse() {
		User jane = saveDirectly();

		ResponseEntity<Map> response = rest.getForEntity("/api/users/" + jane.getId(), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((Map) response.getBody().get("data")).containsEntry("username", "jane");
	}

	// 404 沒有內容，和成功時的 ApiResponse 格式不一致
	@Test
	void findById_shouldReturn404WithoutBody_whenMissing() {
		ResponseEntity<String> response = rest.getForEntity("/api/users/999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();
	}

	// [Potential Bug] catch-all 的 @ExceptionHandler(Exception.class) 把 400 變成 500
	@Test
	void findById_shouldReturn500_whenIdIsNotNumeric() {
		ResponseEntity<Map> response = rest.getForEntity("/api/users/abc", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void delete_shouldReturn200_whenExists_and404_whenMissing() {
		User jane = saveDirectly();

		ResponseEntity<Map> deleted = rest.exchange("/api/users/" + jane.getId(), HttpMethod.DELETE, null, Map.class);
		ResponseEntity<Map> missing = rest.exchange("/api/users/" + jane.getId(), HttpMethod.DELETE, null, Map.class);

		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// [Potential Bug] OpenAPI 的 server 寫死 http://localhost:8080，但應用程式在 8020：Swagger UI 的 Try it out 會打錯 port
	@Test
	void apiDocs_shouldDeclareHardCodedServerOnWrongPort() {
		ResponseEntity<Map> response = rest.getForEntity("/api-docs", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map firstServer = (Map) ((java.util.List) response.getBody().get("servers")).get(0);
		assertThat(firstServer).containsEntry("url", "http://localhost:8080"); // application.yml 的 server.port 是 8020
	}

	// [Potential Bug] application.yml 寫成 name:Spring_Swagger（冒號後沒有空格），應用程式名稱沒有設定
	@Test
	void applicationName_shouldNotBeSet() {
		assertThat(environment.getProperty("spring.application.name")).isNull();
	}
}
