package com.example.demo.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;

/**
 * User / Todo（一對多）API 的行為測試（透過真實 HTTP 呼叫）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class UserTodoApiTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TodoRepository todoRepository;

	@BeforeEach
	void cleanDatabase() {
		todoRepository.deleteAll();
		userRepository.deleteAll();
	}

	private Integer createUser(String name) {
		ResponseEntity<Map> response = restTemplate.postForEntity("/api/users", Map.of("name", name), Map.class);
		return (Integer) response.getBody().get("id");
	}

	private Integer createTodo(Integer userId, String task) {
		ResponseEntity<Map> response = restTemplate.postForEntity("/api/users/{id}/todos", Map.of("task", task),
				Map.class, userId);
		return (Integer) response.getBody().get("id");
	}

	@Test
	void createUser_shouldReturnCreatedWithLocation_whenNameGiven() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/users", Map.of("name", "Tom"),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation().getPath()).startsWith("/api/users/");
		assertThat(response.getBody()).contains("\"name\":\"Tom\"");
		assertThat(userRepository.count()).isEqualTo(1);
	}

	@Test
	void createUser_shouldReturnBadRequest_whenNameMissing() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/users", Map.of(), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("\"name\"");
	}

	@Test
	void createTodo_shouldReturnCreatedWithLocation_whenUserExists() {
		Integer userId = createUser("Tom");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/users/{id}/todos",
				Map.of("task", "Study"), String.class, userId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation().getPath()).startsWith("/api/todos/");
		assertThat(response.getBody()).contains("\"task\":\"Study\"");
	}

	// 使用者在 URL 中：查不到是 404
	@Test
	void createTodo_shouldReturnNotFound_whenUserNotExists() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/users/999999/todos",
				Map.of("task", "Study"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void createTodo_shouldReturnBadRequest_whenTaskBlank() {
		Integer userId = createUser("Tom");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/users/{id}/todos", Map.of("task", " "),
				String.class, userId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void getUser_shouldReturnUserWithTodos_whenUserHasTodos() {
		Integer userId = createUser("Tom");
		createTodo(userId, "Study");

		ResponseEntity<String> response = restTemplate.getForEntity("/api/users/{id}", String.class, userId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"name\":\"Tom\"").contains("\"task\":\"Study\"");
		// User 的 password 欄位仍會出現在回應中（目前決定先不處理）
		assertThat(response.getBody()).contains("\"password\"");
	}

	@Test
	void getUserTodos_shouldReturnTodoList_whenUserHasTodos() {
		Integer userId = createUser("Tom");
		createTodo(userId, "Study");
		createTodo(userId, "Sleep");

		ResponseEntity<Map[]> response = restTemplate.getForEntity("/api/users/{id}/todos", Map[].class, userId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).extracting(todo -> todo.get("task"))
				.containsExactlyInAnyOrder("Study", "Sleep");
	}

	@Test
	void getUser_shouldReturnNotFound_whenUserNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/users/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void getTodo_shouldReturnTodo_whenTodoExists() {
		Integer todoId = createTodo(createUser("Tom"), "Study");

		ResponseEntity<String> response = restTemplate.getForEntity("/api/todos/{id}", String.class, todoId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"task\":\"Study\"").contains("\"status\":1");
	}

	@Test
	void getTodo_shouldReturnNotFound_whenTodoNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/todos/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// 依專案 URL 規則調整後，舊路徑不再提供
	@Test
	void legacyEndpoints_shouldReturnNotFound() {
		assertThat(restTemplate.getForEntity("/todo/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.postForEntity("/saveTodo?task=a&Userid=1", null, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.postForEntity("/api/saveUser?name=a", null, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
