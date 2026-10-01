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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.repository.ImageRepository;

/**
 * 圖片 API 與上傳頁面的行為測試（透過真實 HTTP 呼叫）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ImageApiTest {

	private static final byte[] PNG_BYTES = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 1, 2, 3 };

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		imageRepository.deleteAll();
	}

	private HttpEntity<MultiValueMap<String, Object>> multipartFile(byte[] content) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.IMAGE_PNG);
		body.add("file", new HttpEntity<>(new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return "test.png";
			}
		}, fileHeaders));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		return new HttpEntity<>(body, headers);
	}

	private Long uploadImage() {
		ResponseEntity<Map> response = restTemplate.postForEntity("/api/images", multipartFile(PNG_BYTES), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return ((Number) response.getBody().get("id")).longValue();
	}

	@Test
	void uploadImage_shouldReturnCreatedWithMetadata_whenFileGiven() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/images", multipartFile(PNG_BYTES),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation().getPath()).startsWith("/api/images/");
		assertThat(response.getBody()).contains("\"name\":\"test.png\"").contains("\"size\":" + PNG_BYTES.length)
				.doesNotContain("\"data\"");
	}

	@Test
	void uploadImage_shouldReturnBadRequest_whenFileIsEmpty() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/images", multipartFile(new byte[0]),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(imageRepository.count()).isZero();
	}

	@Test
	void getImage_shouldReturnBytesWithContentType_whenImageUploaded() {
		Long imageId = uploadImage();

		ResponseEntity<byte[]> response = restTemplate.getForEntity("/api/images/{id}", byte[].class, imageId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
		assertThat(response.getBody()).isEqualTo(PNG_BYTES);
	}

	// 修正前：清單會回傳每張圖片的完整內容（base64）
	@Test
	void getAllImages_shouldReturnMetadataWithoutData_whenImagesExist() {
		uploadImage();

		ResponseEntity<String> response = restTemplate.getForEntity("/api/images", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"name\":\"test.png\"").contains("\"contentType\":\"image/png\"")
				.doesNotContain("\"data\"");
	}

	@Test
	void getImage_shouldReturnNotFound_whenImageNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/images/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void deleteImage_shouldReturnNoContent_whenImageExists() {
		Long imageId = uploadImage();

		ResponseEntity<Void> response = restTemplate.exchange("/api/images/{id}", HttpMethod.DELETE, null,
				Void.class, imageId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(imageRepository.existsById(imageId)).isFalse();
	}

	@Test
	void deleteImage_shouldReturnNotFound_whenImageNotExists() {
		ResponseEntity<String> response = restTemplate.exchange("/api/images/999999", HttpMethod.DELETE, null,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// 圖片改存 bytea（修正前為 @Lob，在 PostgreSQL 會存成 oid Large Object）
	@Test
	void imagesTable_shouldStoreDataAsBytea() {
		String type = jdbcTemplate.queryForObject(
				"select data_type from information_schema.columns where table_name = 'images' and column_name = 'data'",
				String.class);

		assertThat(type).isEqualTo("bytea");
	}

	// ===== 上傳頁面（Thymeleaf） =====

	@Test
	void homePage_shouldRenderImageList_whenImagesExist() {
		uploadImage();

		ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
		assertThat(response.getBody()).contains("test.png");
	}

	// 修正前：頁面上的刪除是 GET /delete/{id}；GET 不應修改資料，改為 POST
	@Test
	void deleteImagePage_shouldDeleteImage_whenCalledWithPost() {
		Long imageId = uploadImage();

		restTemplate.postForEntity("/images/{id}/delete", null, String.class, imageId);

		assertThat(imageRepository.existsById(imageId)).isFalse();
	}

	@Test
	void deleteImagePage_shouldNotDeleteImage_whenCalledWithGet() {
		Long imageId = uploadImage();

		restTemplate.getForEntity("/delete/{id}", String.class, imageId);

		assertThat(imageRepository.existsById(imageId)).isTrue();
	}
}
