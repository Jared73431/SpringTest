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
		return multipartFile(content, "test.png");
	}

	private HttpEntity<MultiValueMap<String, Object>> multipartFile(byte[] content, String fileName) {
		return multipartFile(content, fileName, MediaType.IMAGE_PNG);
	}

	private HttpEntity<MultiValueMap<String, Object>> multipartFile(byte[] content, String fileName,
			MediaType contentType) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(contentType);
		body.add("file", new HttpEntity<>(new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return fileName;
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

	// 顯示（/api/images/{id}）是 inline；下載（/download）是 attachment，瀏覽器會存成檔案
	@Test
	void downloadImage_shouldReturnAttachmentWithFileName_whenImageUploaded() {
		Long imageId = uploadImage();

		ResponseEntity<byte[]> response = restTemplate.getForEntity("/api/images/{id}/download", byte[].class, imageId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentDisposition().isAttachment()).isTrue();
		assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("test.png");
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
		assertThat(response.getBody()).isEqualTo(PNG_BYTES);
	}

	// 中文檔名：用 RFC 5987 的 filename*=UTF-8''... 編碼，瀏覽器才不會顯示亂碼
	@Test
	void downloadImage_shouldEncodeNonAsciiFileName() {
		ResponseEntity<Map> uploaded = restTemplate.postForEntity("/api/images", multipartFile(PNG_BYTES, "貓.png"),
				Map.class);
		Long imageId = ((Number) uploaded.getBody().get("id")).longValue();

		ResponseEntity<byte[]> response = restTemplate.getForEntity("/api/images/{id}/download", byte[].class, imageId);

		assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
				.contains("filename*=UTF-8''%E8%B2%93.png");
		assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("貓.png");
	}

	@Test
	void displayImage_shouldNotBeAnAttachment() {
		Long imageId = uploadImage();

		ResponseEntity<byte[]> response = restTemplate.getForEntity("/api/images/{id}", byte[].class, imageId);

		assertThat(response.getHeaders().getContentDisposition().isAttachment()).isFalse();
	}

	@Test
	void downloadImage_shouldReturnNotFound_whenImageNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/images/999999/download", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void homePage_shouldLinkToDownload() {
		Long imageId = uploadImage();

		ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

		assertThat(response.getBody()).contains("/api/images/" + imageId + "/download");
	}

	// ===== 上傳的檔案類型（修正前：任何檔案都能上傳，並用上傳者給的 Content-Type 直接顯示 → Stored XSS） =====

	private static final byte[] HTML_BYTES = "<html><script>alert(document.cookie)</script></html>"
			.getBytes(java.nio.charset.StandardCharsets.UTF_8);

	private static final byte[] SVG_BYTES = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
			.getBytes(java.nio.charset.StandardCharsets.UTF_8);

	@Test
	void uploadImage_shouldReturnBadRequest_whenFileIsHtml() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/images",
				multipartFile(HTML_BYTES, "evil.html", MediaType.TEXT_HTML), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(imageRepository.count()).isZero();
	}

	// 副檔名與 Content-Type 都由上傳的人決定，可以偽裝：要看檔案內容
	@Test
	void uploadImage_shouldReturnBadRequest_whenHtmlIsDisguisedAsPng() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/images",
				multipartFile(HTML_BYTES, "cat.png", MediaType.IMAGE_PNG), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(imageRepository.count()).isZero();
	}

	// SVG 是 XML，可以包含 script：不接受
	@Test
	void uploadImage_shouldReturnBadRequest_whenFileIsSvg() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/images",
				multipartFile(SVG_BYTES, "logo.svg", MediaType.parseMediaType("image/svg+xml")), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(imageRepository.count()).isZero();
	}

	// 存的 Content-Type 由檔案內容判斷，不使用上傳者給的值
	@Test
	void uploadImage_shouldStoreTheDetectedContentType_whenDeclaredTypeIsWrong() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/images",
				multipartFile(PNG_BYTES, "photo.jpg", MediaType.IMAGE_JPEG), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).contains("\"contentType\":\"image/png\"");
	}

	// nosniff：瀏覽器不會自己「猜」內容是 HTML 而執行
	@Test
	void getAndDownloadImage_shouldSendNosniff() {
		Long imageId = uploadImage();

		assertThat(restTemplate.getForEntity("/api/images/{id}", byte[].class, imageId).getHeaders()
				.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
		assertThat(restTemplate.getForEntity("/api/images/{id}/download", byte[].class, imageId).getHeaders()
				.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
	}

	// 修正前已經存進資料庫的非圖片檔案：不再 inline 顯示，改成下載
	@Test
	void getImage_shouldNotDisplayInline_whenStoredTypeIsNotAnAllowedImage() {
		Long imageId = imageRepository.save(new com.example.demo.entity.Image("old.html", "text/html", HTML_BYTES))
				.getId();

		ResponseEntity<byte[]> response = restTemplate.getForEntity("/api/images/{id}", byte[].class, imageId);

		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
		assertThat(response.getHeaders().getContentDisposition().isAttachment()).isTrue();
	}

	// 上傳頁面：不是圖片時顯示訊息，不會出現錯誤頁
	@Test
	void uploadPage_shouldNotStoreTheFile_whenFileIsNotAnImage() {
		ResponseEntity<String> response = restTemplate.postForEntity("/upload",
				multipartFile(HTML_BYTES, "evil.html", MediaType.TEXT_HTML), String.class);

		assertThat(response.getStatusCode().is5xxServerError()).isFalse();
		assertThat(imageRepository.count()).isZero();
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
