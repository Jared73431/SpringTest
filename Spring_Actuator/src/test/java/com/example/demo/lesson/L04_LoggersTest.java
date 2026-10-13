package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

import com.example.demo.ActuatorTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 4 課：不重新啟動，就改變 log level。
 *
 * <pre>
 * GET  /actuator/loggers/{name}                        目前的 level
 * POST /actuator/loggers/{name}  {"configuredLevel":"DEBUG"}   改成 DEBUG（204）
 * POST /actuator/loggers/{name}  {"configuredLevel":null}      恢復原本的設定
 * </pre>
 *
 * 線上有問題時，暫時打開某個 package 的 DEBUG，查完再關掉。改變只在記憶體中，重新啟動後恢復設定檔的值。
 */
@ExtendWith(OutputCaptureExtension.class)
class L04_LoggersTest extends ActuatorTestBase {

	private static final String LOGGER = "/actuator/loggers/com.example.demo.book";

	private void setLevel(String level) {
		String body = level == null ? "{\"configuredLevel\":null}" : "{\"configuredLevel\":\"" + level + "\"}";
		var status = management().post().uri(LOGGER).contentType(MediaType.APPLICATION_JSON).body(body).retrieve()
				.toBodilessEntity().getStatusCode();
		assertThat(status.value()).isEqualTo(204);
	}

	@Test
	void logLevel_shouldBeChangeableAtRuntime(CapturedOutput output) {
		// 預設是 INFO（從 root 繼承）：DEBUG 的訊息不會出現
		String before = management().get().uri(LOGGER).retrieve().body(String.class);
		assertThat((String) JsonPath.read(before, "$.effectiveLevel")).isEqualTo("INFO");
		api().get().uri("/api/books?title=effective").retrieve().toBodilessEntity();
		assertThat(output).doesNotContain("Searching books by title containing 'effective'");

		try {
			setLevel("DEBUG");
			String after = management().get().uri(LOGGER).retrieve().body(String.class);
			assertThat((String) JsonPath.read(after, "$.configuredLevel")).isEqualTo("DEBUG");

			api().get().uri("/api/books?title=spring").retrieve().toBodilessEntity();
			assertThat(output).contains("Searching books by title containing 'spring'");
		}
		finally {
			setLevel(null);
		}
	}
}
