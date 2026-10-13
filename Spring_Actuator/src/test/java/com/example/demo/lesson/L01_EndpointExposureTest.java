package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.demo.ActuatorTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 1 課：Actuator 有哪些端點、透過 HTTP 公開哪些。
 *
 * <pre>
 * 端點存在（enabled）≠ 透過 HTTP 公開（exposed）
 *
 * 預設：只公開 health
 * 本模組：management.endpoints.web.exposure.include=health,info,metrics,prometheus,loggers
 * 不公開：env（設定值，可能有密碼）、configprops、heapdump（記憶體內容）、threaddump、shutdown
 * </pre>
 */
class L01_EndpointExposureTest extends ActuatorTestBase {

	// /actuator 列出公開的端點
	@Test
	void actuatorRoot_shouldListOnlyTheExposedEndpoints() {
		String body = management().get().uri("/actuator").retrieve().body(String.class);

		Map<String, Object> links = JsonPath.read(body, "$._links");
		assertThat(links).containsKeys("health", "info", "metrics", "prometheus", "loggers")
				.doesNotContainKeys("env", "configprops", "heapdump", "threaddump", "shutdown", "beans");
	}

	@Test
	void health_shouldBeUp() {
		var response = management().get().uri("/actuator/health").retrieve().toEntity(String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat((String) JsonPath.read(response.getBody(), "$.status")).isEqualTo("UP");
	}

	// 沒有公開的端點：404，和不存在一樣
	@Test
	void sensitiveEndpoints_shouldNotBeExposed() {
		for (String path : new String[] { "/actuator/env", "/actuator/heapdump", "/actuator/configprops", "/actuator/shutdown" }) {
			var status = management().get().uri(path).retrieve().toBodilessEntity().getStatusCode();
			assertThat(status.value()).as(path).isEqualTo(404);
		}
	}
}
