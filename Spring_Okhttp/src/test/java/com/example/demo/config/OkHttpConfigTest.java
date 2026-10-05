package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * 單元測試（不啟動 Spring）：日誌等級調到 HEADERS 時，Authorization header 的值會被遮蔽，不會寫進 log。
 */
@ExtendWith(OutputCaptureExtension.class)
class OkHttpConfigTest {

	@Test
	void logging_shouldRedactAuthorizationHeader(CapturedOutput output) throws Exception {
		JsonPlaceholderProperties properties = new JsonPlaceholderProperties("http://unused", Duration.ofSeconds(2),
				Duration.ofSeconds(5), Duration.ofSeconds(5), HttpLoggingInterceptor.Level.HEADERS);
		OkHttpClient client = new OkHttpConfig().okHttpClient(properties);

		try (MockWebServer server = new MockWebServer()) {
			server.enqueue(new MockResponse.Builder().code(200).body("ok").build());
			server.start();
			Request request = new Request.Builder().url(server.url("/secret"))
					.header("Authorization", "Bearer my-secret-token").build();
			try (Response response = client.newCall(request).execute()) {
				assertThat(response.code()).isEqualTo(200);
			}
		}

		assertThat(output).contains("Authorization: ").doesNotContain("my-secret-token");
	}
}
