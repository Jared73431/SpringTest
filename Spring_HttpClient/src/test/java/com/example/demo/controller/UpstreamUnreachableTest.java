package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

/**
 * 外部 API 完全連不上（port 1 沒有服務在監聽）時：
 * <ul>
 * <li>應用程式仍可啟動：DemoRunner 開啟但呼叫失敗，只記錄警告（修正前 block() 拋出例外，啟動失敗）</li>
 * <li>兩個版本的 API 都回 503</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"jsonplaceholder.base-url=http://localhost:1",
		"demo.runner.enabled=true" })
@AutoConfigureTestRestTemplate
@ExtendWith(OutputCaptureExtension.class)
class UpstreamUnreachableTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void application_shouldStart_whenDemoRunnerFails(CapturedOutput output) {
		// 能執行到這裡就代表 context 已成功啟動；DemoRunner 的警告在啟動時輸出
		assertThat(restTemplate.getForEntity("/api/posts/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void bothApis_shouldReturnServiceUnavailable_whenUpstreamUnreachable() {
		assertThat(restTemplate.getForEntity("/api/posts/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(restTemplate.getForEntity("/api/reactive/posts/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	// 舊的測試端點（/test/...）已移除
	@Test
	void legacyTestEndpoints_shouldReturnNotFound() {
		assertThat(restTemplate.getForEntity("/test/get/1", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(restTemplate.getForEntity("/test/create-sample", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
