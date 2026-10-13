package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.example.demo.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 3 課：使用者上傳的檔案都不可信。
 *
 * <ul>
 * <li>檔名：可能是 "../../xxx" 這類路徑 → 磁碟上一律用新產生的 UUID 當檔名</li>
 * <li>Content-Type：上傳時由客戶端宣稱，可以偽造 → 下載時由伺服器依副檔名決定，不認得的一律 application/octet-stream</li>
 * <li>內容：可能是 HTML → 一律當成附件下載（attachment）+ nosniff，不在我們的網站上打開</li>
 * <li>大小、空檔案 → 413、400</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L03_SafeStorageTest extends IntegrationTestBase {

	@Test
	void pathInTheFileName_shouldNotLeaveTheStorageDirectory() throws Exception {
		var response = upload("../../escape.txt", "trying to escape");

		String id = JsonPath.read(response.getBody(), "$.id");
		assertThat((String) JsonPath.read(response.getBody(), "$.name")).isEqualTo("escape.txt");
		assertThat(Files.exists(STORAGE.getParent().getParent().resolve("escape.txt"))).isFalse();
		// 存放目錄中只有 UUID 檔名的檔案
		try (var files = Files.list(STORAGE)) {
			assertThat(files.map(path -> path.getFileName().toString()))
					.allSatisfy(name -> assertThat(UUID.fromString(name)).isNotNull());
		}
		assertThat(http().get().uri("/api/files/{id}/content", id).retrieve().body(String.class))
				.isEqualTo("trying to escape");
	}

	// 宣稱是 text/html 的 HTML 檔：下載時是 application/octet-stream 的附件，瀏覽器只會存檔，不會執行
	@Test
	void htmlFile_shouldBeDownloadedAsAnOpaqueAttachment() {
		var response = upload("page.html", "text/html", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8),
				"alice");
		String id = JsonPath.read(response.getBody(), "$.id");

		var download = http().get().uri("/api/files/{id}/content", id).retrieve().toEntity(String.class);

		assertThat(download.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
		assertThat(download.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment;");
		assertThat(download.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
	}

	@Test
	void emptyFile_shouldBeRejectedWith400() {
		var response = upload("empty.txt", "");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat((String) JsonPath.read(response.getBody(), "$.detail")).isEqualTo("File must not be empty");
	}

	@Test
	void invalidUploaderName_shouldBeRejectedWith400() {
		var response = upload("a.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8), "<b>me</b>");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	// 實測：Tomcat 預設只會讀掉 2MB（max-swallow-size）還沒送完的內容，超過就直接關閉連線，
	// 客戶端收到的是「連線中斷」而不是 413。設定 server.tomcat.max-swallow-size=10MB 後才收得到 413
	@Test
	void fileLargerThan5MB_shouldBeRejectedWith413() {
		var response = upload("big.bin", "application/octet-stream", new byte[6 * 1024 * 1024], "alice");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
	}

	@Test
	void unknownFile_shouldBe404() {
		var response = http().get().uri("/api/files/{id}/content", UUID.randomUUID()).retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
