package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.ActuatorTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 2 課（續）：自訂的檢查回報 DOWN 時。
 *
 * 封面目錄不存在 → coverStorage DOWN → 整體 DOWN（503）、readiness DOWN（503）；
 * liveness 仍然 UP：目錄不見了，重新啟動應用程式也不會好。
 */
@TestPropertySource(properties = "app.cover-storage.dir=/path/that/does/not/exist")
class L02_HealthDownTest extends ActuatorTestBase {

	@Test
	void missingDirectory_shouldMakeHealthAndReadinessDown() {
		var health = management().get().uri("/actuator/health").retrieve().toEntity(String.class);

		assertThat(health.getStatusCode().value()).isEqualTo(503);
		assertThat((String) JsonPath.read(health.getBody(), "$.status")).isEqualTo("DOWN");
		assertThat((String) JsonPath.read(health.getBody(), "$.components.coverStorage.status")).isEqualTo("DOWN");
		assertThat((String) JsonPath.read(health.getBody(), "$.components.coverStorage.details.error"))
				.contains("does not exist");
		assertThat((String) JsonPath.read(health.getBody(), "$.components.db.status")).isEqualTo("UP");

		assertThat(management().get().uri("/actuator/health/readiness").retrieve().toBodilessEntity()
				.getStatusCode().value()).isEqualTo(503);
		assertThat(management().get().uri("/actuator/health/liveness").retrieve().toBodilessEntity()
				.getStatusCode().value()).isEqualTo(200);
	}
}
