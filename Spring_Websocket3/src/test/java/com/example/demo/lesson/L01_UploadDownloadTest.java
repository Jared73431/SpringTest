package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.example.demo.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 1 課：檔案的上傳與下載走 HTTP。
 *
 * <p>
 * 為什麼不用 WebSocket 傳檔案：HTTP 已經處理好大小限制、multipart、下載的檔名與類型、快取、續傳；
 * WebSocket 適合「有新檔案」這種小而即時的通知（第 2 課）。
 *
 * <p>
 * 檔案在磁碟上的位置記在資料庫，下載時用資料庫的紀錄找：修正前到「今天、昨天、前天」的日期目錄找，
 * 3 天前上傳的檔案就下載不到（baseline 確認）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class L01_UploadDownloadTest extends IntegrationTestBase {

	@Autowired
	private JdbcClient jdbc;

	@Test
	void upload_shouldReturn201WithTheLocationOfTheFile() {
		var response = upload("hello.txt", "hello file");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String id = JsonPath.read(response.getBody(), "$.id");
		assertThat(response.getHeaders().getLocation()).hasToString("/api/files/" + id);
		assertThat((String) JsonPath.read(response.getBody(), "$.name")).isEqualTo("hello.txt");
		assertThat((Integer) JsonPath.read(response.getBody(), "$.size")).isEqualTo(10);
		assertThat((String) JsonPath.read(response.getBody(), "$.uploadedBy")).isEqualTo("alice");
	}

	@Test
	void download_shouldReturnTheSameContent_asAnAttachmentWithTheOriginalName() {
		String id = JsonPath.read(upload("報告.txt", "hello file").getBody(), "$.id");

		var response = http().get().uri("/api/files/{id}/content", id).retrieve().toEntity(String.class);

		assertThat(response.getBody()).isEqualTo("hello file");
		assertThat(response.getHeaders().getContentType()).hasToString("text/plain");
		// 中文檔名用 RFC 5987 的 filename* 編碼
		assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
				.startsWith("attachment;").contains("filename*=UTF-8''%E5%A0%B1%E5%91%8A.txt");
	}

	@Test
	void list_shouldShowTheNewestFirst() {
		upload("first.txt", "1");
		upload("second.txt", "2");

		var body = http().get().uri("/api/files").retrieve().body(String.class);

		assertThat((String) JsonPath.read(body, "$[0].name")).isEqualTo("second.txt");
	}

	@Test
	void oldFile_shouldStillBeDownloadable() {
		String id = JsonPath.read(upload("old.txt", "from last month").getBody(), "$.id");
		jdbc.sql("UPDATE shared_file SET uploaded_at = now() - interval '30 days' WHERE id = :id")
				.param("id", UUID.fromString(id)).update();

		var response = http().get().uri("/api/files/{id}/content", id).retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("from last month");
	}
}
