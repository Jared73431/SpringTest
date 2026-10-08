package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.repository.UserRepository;

/**
 * /api/users 的 API 測試（取代修正前的 baseline，註解保留修正前的行為）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@SuppressWarnings({ "rawtypes", "unchecked" })
class UserApiTest extends PostgresContainerTestBase {

	private static final UserRequest JOHN = new UserRequest("john_doe", "John Doe", "john@example.com", 25);

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void cleanTable() {
		userRepository.deleteAll();
	}

	private UserResponse create(UserRequest request) {
		ResponseEntity<UserResponse> response = rest.postForEntity("/api/users", request, UserResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody();
	}

	// 修正前：JSON body 沒有被讀取（Swagger 的 @RequestBody），存入一筆欄位全部是 null 的資料，回傳 200
	@Test
	void create_shouldReturn201WithLocationAndSaveAllFields() {
		ResponseEntity<UserResponse> response = rest.postForEntity("/api/users", JOHN, UserResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasToString("/api/users/" + response.getBody().id());
		assertThat(response.getBody()).isEqualTo(
				new UserResponse(response.getBody().id(), "john_doe", "John Doe", "john@example.com", 25));
		assertThat(userRepository.findAll()).singleElement()
				.satisfies(user -> assertThat(user.getEmail()).isEqualTo("john@example.com"));
	}

	// 修正前：回應只有「參數驗證失敗」，沒有說是哪些欄位
	@Test
	void create_shouldReturn400ProblemDetailWithFieldErrors_whenInvalid() {
		ResponseEntity<Map> response = rest.postForEntity("/api/users",
				new UserRequest(" ", "John", "not-an-email", 200), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat((Map) response.getBody().get("errors"))
				.containsEntry("username", "帳號不可空白")
				.containsEntry("email", "電子郵件格式不正確")
				.containsEntry("age", "年齡不可大於 150");
	}

	// 修正前：違反 unique 限制時由 catch-all 回傳 500
	@Test
	void create_shouldReturn409_whenUsernameIsTaken() {
		create(JOHN);

		ResponseEntity<Map> response = rest.postForEntity("/api/users",
				new UserRequest("john_doe", "Another John", "another@example.com", 30), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).containsEntry("detail", "帳號 已被使用：john_doe");
	}

	@Test
	void create_shouldReturn409_whenEmailIsTaken() {
		create(JOHN);

		ResponseEntity<Map> response = rest.postForEntity("/api/users",
				new UserRequest("another", "Another", "john@example.com", 30), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	// 修正前：成功時用 ApiResponse（code、message、data）包起來；現在直接回傳資源
	@Test
	void findAll_shouldReturnPlainList() {
		create(JOHN);

		ResponseEntity<List> response = rest.getForEntity("/api/users", List.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(1);
	}

	@Test
	void findById_shouldReturnUser_whenExists() {
		UserResponse john = create(JOHN);

		assertThat(rest.getForEntity("/api/users/" + john.id(), UserResponse.class).getBody()).isEqualTo(john);
	}

	// 修正前：404 沒有內容
	@Test
	void findById_shouldReturn404ProblemDetail_whenMissing() {
		ResponseEntity<Map> response = rest.getForEntity("/api/users/999", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("detail", "找不到使用者：999");
	}

	// 修正前：catch-all 回傳 500
	@Test
	void findById_shouldReturn400_whenIdIsNotNumeric() {
		assertThat(rest.getForEntity("/api/users/abc", Map.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	// 修正前：JSON body 沒有被讀取，驗證失敗回傳 400（更早之前沒有驗證實作時，會把資料清成 null）
	@Test
	void update_shouldChangeUser() {
		UserResponse john = create(JOHN);

		ResponseEntity<UserResponse> response = rest.exchange("/api/users/" + john.id(), HttpMethod.PUT,
				new HttpEntity<>(new UserRequest("john_doe", "Johnny", "johnny@example.com", 26)), UserResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().name()).isEqualTo("Johnny");
		assertThat(userRepository.findById(john.id()).orElseThrow().getEmail()).isEqualTo("johnny@example.com");
	}

	@Test
	void update_shouldReturn409_whenEmailBelongsToAnotherUser() {
		UserResponse john = create(JOHN);
		create(new UserRequest("jane", "Jane", "jane@example.com", 30));

		ResponseEntity<Map> response = rest.exchange("/api/users/" + john.id(), HttpMethod.PUT,
				new HttpEntity<>(new UserRequest("john_doe", "John", "jane@example.com", 25)), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void update_shouldReturn404_whenMissing() {
		ResponseEntity<Map> response = rest.exchange("/api/users/999", HttpMethod.PUT, new HttpEntity<>(JOHN), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// 修正前：200 + ApiResponse
	@Test
	void delete_shouldReturn204_whenExists_and404_whenMissing() {
		UserResponse john = create(JOHN);

		ResponseEntity<Void> deleted = rest.exchange("/api/users/" + john.id(), HttpMethod.DELETE, null, Void.class);
		ResponseEntity<Map> missing = rest.exchange("/api/users/" + john.id(), HttpMethod.DELETE, null, Map.class);

		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
