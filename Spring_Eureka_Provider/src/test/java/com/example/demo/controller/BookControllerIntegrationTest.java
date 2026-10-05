package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.example.demo.entity.Book;
import com.example.demo.repository.BookRepo;

/**
 * 書籍 API 的整合測試：以隨機 port 啟動完整應用程式，透過真實 HTTP（TestRestTemplate）呼叫 API，
 * 資料庫為 Testcontainers 啟動的臨時 PostgreSQL（見 PostgresContainerTestBase）。
 * 每個測試前清空資料表，測試之間互不影響。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class BookControllerIntegrationTest extends PostgresContainerTestBase {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private BookRepo bookRepo;

	@BeforeEach
	void cleanDatabase() {
		bookRepo.deleteAll();
	}

	@Test
	void hello_shouldReturnInstanceId() {
		assertThat(restTemplate.getForObject("/api/hello", String.class)).isEqualTo("Hello from test-instance");
	}

	private Book saveBook(int isbn, String title) {
		return bookRepo.save(new BookRequest(isbn, title, "Tom", 2020, "OReilly", 450.5).toEntity());
	}

	private ResponseEntity<String> postJson(String url, String json) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
	}

	@Test
	void findAll_shouldReturnEmptyList_whenNoBooks() {
		ResponseEntity<BookResponse[]> response = restTemplate.getForEntity("/api/books", BookResponse[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEmpty();
	}

	@Test
	void findAll_shouldReturnAllBooks_whenBooksExist() {
		saveBook(12345, "Java");
		saveBook(67890, "Spring");

		ResponseEntity<BookResponse[]> response = restTemplate.getForEntity("/api/books", BookResponse[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).extracting(BookResponse::title).containsExactlyInAnyOrder("Java", "Spring");
	}

	@Test
	void findById_shouldReturnBook_whenIdExists() {
		Book saved = saveBook(12345, "Java");

		ResponseEntity<BookResponse> response = restTemplate.getForEntity("/api/books/{id}", BookResponse.class,
				saved.getId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(new BookResponse(saved.getId(), 12345, "Java", "Tom", 2020,
				"OReilly", 450.5));
	}

	@Test
	void findById_shouldReturnNotFoundProblemDetail_whenIdNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/books/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).contains("999999");
	}

	@Test
	void create_shouldReturnCreatedWithLocation_whenRequestIsValid() {
		BookRequest request = new BookRequest(12345, "Java", "Tom", 2020, "OReilly", 450.5);

		ResponseEntity<BookResponse> response = restTemplate.postForEntity("/api/books", request,
				BookResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer id = response.getBody().id();
		assertThat(response.getHeaders().getLocation()).hasPath("/api/books/" + id);
		assertThat(response.getBody().title()).isEqualTo("Java");
		Book saved = bookRepo.findById(id).orElseThrow();
		assertThat(saved.getISBN()).isEqualTo(12345);
		assertThat(saved.getCost()).isEqualTo(450.5);
	}

	@Test
	void create_shouldReturnBadRequestProblemDetail_whenRequiredFieldMissing() {
		ResponseEntity<String> response = postJson("/api/books", "{\"isbn\": 1, \"title\": \"T\"}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(bookRepo.count()).isZero();
	}

	// ISBN 使用 Integer，超過 Integer 範圍的 13 碼 ISBN 無法存入（練習用設計，刻意保留）
	@Test
	void create_shouldReturnBadRequest_whenIsbnExceedsIntegerRange() {
		ResponseEntity<String> response = postJson("/api/books", """
				{"isbn": 9780134685991, "title": "T", "author": "A", "year": 2018, "publisher": "P", "cost": 1}
				""");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(bookRepo.count()).isZero();
	}

	@Test
	void update_shouldUpdateExistingRow_whenIdExists() {
		Book saved = saveBook(12345, "Java");
		BookRequest request = new BookRequest(12345, "Java 2nd", "Tom", 2021, "OReilly", 500.0);

		ResponseEntity<BookResponse> response = restTemplate.exchange("/api/books/{id}", HttpMethod.PUT,
				new HttpEntity<>(request), BookResponse.class, saved.getId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().id()).isEqualTo(saved.getId());
		assertThat(response.getBody().title()).isEqualTo("Java 2nd");
		assertThat(bookRepo.count()).isEqualTo(1);
		Book updated = bookRepo.findById(saved.getId()).orElseThrow();
		assertThat(updated.getTitle()).isEqualTo("Java 2nd");
		assertThat(updated.getYear()).isEqualTo(2021);
		assertThat(updated.getCost()).isEqualTo(500);
	}

	@Test
	void update_shouldReturnNotFound_whenIdNotExists() {
		BookRequest request = new BookRequest(11111, "Ghost", "X", 2000, "P", 1.0);

		ResponseEntity<String> response = restTemplate.exchange("/api/books/999999", HttpMethod.PUT,
				new HttpEntity<>(request), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(bookRepo.count()).isZero();
	}

	@Test
	void delete_shouldReturnNoContentAndRemoveBook_whenIdExists() {
		Book saved = saveBook(12345, "Java");

		ResponseEntity<Void> response = restTemplate.exchange("/api/books/{id}", HttpMethod.DELETE, null,
				Void.class, saved.getId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(bookRepo.existsById(saved.getId())).isFalse();
	}

	@Test
	void delete_shouldReturnNotFound_whenIdNotExists() {
		ResponseEntity<String> response = restTemplate.exchange("/api/books/999999", HttpMethod.DELETE, null,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void findAll_shouldReturnMethodNotAllowed_whenPatchOnCollection() {
		ResponseEntity<String> response = restTemplate.exchange("/api/books", HttpMethod.PATCH, null, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	}

	// 依專案 URL 規則改為 /api/books，舊路徑不再提供
	@Test
	void legacyEndpoints_shouldReturnNotFound() {
		assertThat(restTemplate.getForEntity("/findall", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForEntity("/hello", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForEntity("/getOneBook/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
