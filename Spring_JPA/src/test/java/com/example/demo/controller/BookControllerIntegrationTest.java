package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.entity.Book;
import com.example.demo.repository.BookRepo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookControllerIntegrationTest extends PostgresContainerTestBase {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private BookRepo bookRepo;

	@BeforeEach
	void cleanDatabase() {
		bookRepo.deleteAll();
	}

	private Book saveBook(int isbn, String title) {
		Book book = new Book();
		book.setISBN(isbn);
		book.setTitle(title);
		book.setAuthor("Tom");
		book.setYear(2020);
		book.setPublisher("OReilly");
		book.setCost(450.5);
		return bookRepo.save(book);
	}

	@Test
	void findall_shouldReturnEmptyList_whenNoBooks() {
		ResponseEntity<Book[]> response = restTemplate.getForEntity("/findall", Book[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEmpty();
	}

	@Test
	void findall_shouldReturnAllBooks_whenBooksExist() {
		saveBook(12345, "Java");
		saveBook(67890, "Spring");

		ResponseEntity<Book[]> response = restTemplate.getForEntity("/findall", Book[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).extracting(Book::getTitle).containsExactlyInAnyOrder("Java", "Spring");
	}

	@Test
	void saveBook_shouldPersistBookAndReturnEmptyBody_whenAllParamsGiven() {
		ResponseEntity<String> response = restTemplate.postForEntity(
				"/saveBook?ISBN=12345&title=Java&author=Tom&year=2020&publisher=OReilly&cost=450.5", null,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNull();
		List<Book> books = bookRepo.findAll();
		assertThat(books).hasSize(1);
		assertThat(books.get(0).getISBN()).isEqualTo(12345);
		assertThat(books.get(0).getTitle()).isEqualTo("Java");
		assertThat(books.get(0).getCost()).isEqualTo(450.5);
	}

	@Test
	void saveBook_shouldReturnBadRequest_whenParamMissing() {
		ResponseEntity<String> response = restTemplate.postForEntity("/saveBook?ISBN=1&title=T", null, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(bookRepo.count()).isZero();
	}

	// ISBN 使用 Integer，超過 Integer 範圍的 13 碼 ISBN 無法存入（練習用設計，刻意保留）
	@Test
	void saveBook_shouldReturnBadRequest_whenIsbnExceedsIntegerRange() {
		ResponseEntity<String> response = restTemplate.postForEntity(
				"/saveBook?ISBN=9780134685991&title=T&author=A&year=2018&publisher=P&cost=1", null, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void getOneBook_shouldReturnBook_whenIdExists() {
		Book saved = saveBook(12345, "Java");

		ResponseEntity<Book> response = restTemplate.getForEntity("/getOneBook/" + saved.getId(), Book.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getTitle()).isEqualTo("Java");
	}

	// [Potential Bug] 目前查不到資料時回 500，預計修正為 404
	@Test
	void getOneBook_shouldReturnInternalServerError_whenIdNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/getOneBook/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	// [Potential Bug] 目前「更新」不會修改原資料，而是新增一筆，預計修正
	@Test
	void updateBook_shouldInsertNewRowAndKeepOriginal_whenIdExists() {
		Book saved = saveBook(12345, "Java");

		ResponseEntity<Book> response = restTemplate.postForEntity("/updateBook?ID=" + saved.getId()
				+ "&ISBN=12345&title=Java 2nd&author=Tom&year=2021&publisher=OReilly&cost=500", null, Book.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getId()).isNotEqualTo(saved.getId());
		assertThat(response.getBody().getTitle()).isEqualTo("Java 2nd");
		assertThat(bookRepo.count()).isEqualTo(2);
		assertThat(bookRepo.findById(saved.getId()).orElseThrow().getTitle()).isEqualTo("Java");
	}

	// [Potential Bug] 目前「更新」不存在的 id 也會新增一筆，預計修正
	@Test
	void updateBook_shouldInsertNewRow_whenIdNotExists() {
		ResponseEntity<Book> response = restTemplate.postForEntity(
				"/updateBook?ID=999999&ISBN=11111&title=Ghost&author=X&year=2000&publisher=P&cost=1", null,
				Book.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getId()).isNotEqualTo(999999);
		assertThat(bookRepo.count()).isEqualTo(1);
	}

	@Test
	void saveBook_shouldReturnMethodNotAllowed_whenGetRequest() {
		ResponseEntity<String> response = restTemplate.getForEntity("/saveBook", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	}
}
