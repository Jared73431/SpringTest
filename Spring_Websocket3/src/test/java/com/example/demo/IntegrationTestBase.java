package com.example.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 整合測試共用：PostgreSQL container、暫存的檔案目錄、上傳用的 RestClient。
 */
public abstract class IntegrationTestBase {

	@ServiceConnection
	protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	/** 檔案存到暫存目錄，不會在專案目錄中產生檔案 */
	protected static final Path STORAGE;

	static {
		POSTGRES.start();
		try {
			STORAGE = Files.createTempDirectory("shared-files");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@DynamicPropertySource
	static void storageDirectory(DynamicPropertyRegistry registry) {
		registry.add("app.storage.directory", STORAGE::toString);
	}

	@LocalServerPort
	protected int port;

	/** 4xx、5xx 不拋例外，直接回傳，讓測試檢查錯誤回應 */
	protected RestClient http() {
		return RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.defaultStatusHandler(status -> true, (request, response) -> {
				})
				.build();
	}

	protected ResponseEntity<String> upload(String fileName, String contentType, byte[] content, String uploadedBy) {
		var fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.parseMediaType(contentType));
		var body = new LinkedMultiValueMap<String, Object>();
		body.add("file", new HttpEntity<>(new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return fileName;
			}
		}, fileHeaders));
		body.add("uploadedBy", uploadedBy);
		return http().post().uri("/api/files").contentType(MediaType.MULTIPART_FORM_DATA).body(body)
				.retrieve().toEntity(String.class);
	}

	protected ResponseEntity<String> upload(String fileName, String text) {
		return upload(fileName, "text/plain", text.getBytes(StandardCharsets.UTF_8), "alice");
	}
}
