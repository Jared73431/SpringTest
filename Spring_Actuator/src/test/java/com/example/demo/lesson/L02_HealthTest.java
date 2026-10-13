package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;

import com.example.demo.ActuatorTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 2 課：Health —— 整體、各項檢查、liveness 與 readiness。
 *
 * <pre>
 * /actuator/health              整體：任何一項 DOWN 就是 DOWN（HTTP 503）
 *   ├─ db                       資料庫（Spring Boot 自動加上）
 *   ├─ diskSpace                磁碟空間（自動）
 *   ├─ coverStorage             自訂的 CoverStorageHealthIndicator
 *   ├─ livenessState / readinessState
 *   └─ ping
 * /actuator/health/liveness     「活著嗎？」  失敗 → K8S 重新啟動容器
 * /actuator/health/readiness    「能接流量嗎？」失敗 → K8S 先不把流量送過來（不重新啟動）
 * </pre>
 *
 * 資料庫斷線時重新啟動也沒用，所以 db 只放在 readiness，不放在 liveness。
 */
class L02_HealthTest extends ActuatorTestBase {

	@Autowired
	private ApplicationContext context;

	@Test
	void health_shouldShowEveryComponent() {
		String body = management().get().uri("/actuator/health").retrieve().body(String.class);

		Map<String, Object> components = JsonPath.read(body, "$.components");
		assertThat(components).containsKeys("db", "diskSpace", "coverStorage", "livenessState", "readinessState");
		assertThat((String) JsonPath.read(body, "$.components.db.status")).isEqualTo("UP");
		assertThat((String) JsonPath.read(body, "$.components.db.details.database")).isEqualTo("PostgreSQL");
		assertThat((String) JsonPath.read(body, "$.components.coverStorage.status")).isEqualTo("UP");
	}

	// 一個檢查項目也可以單獨查詢
	@Test
	void singleComponent_shouldBeQueryable() {
		String body = management().get().uri("/actuator/health/coverStorage").retrieve().body(String.class);

		assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("UP");
		assertThat((String) JsonPath.read(body, "$.details.directory")).isNotBlank();
	}

	// readiness 群組包含 db 與 coverStorage；liveness 只有 livenessState
	@Test
	void probes_shouldIncludeTheConfiguredComponents() {
		String readiness = management().get().uri("/actuator/health/readiness").retrieve().body(String.class);
		String liveness = management().get().uri("/actuator/health/liveness").retrieve().body(String.class);

		Map<String, Object> readinessComponents = JsonPath.read(readiness, "$.components");
		assertThat(readinessComponents).containsOnlyKeys("readinessState", "db", "coverStorage");
		assertThat((String) JsonPath.read(liveness, "$.status")).isEqualTo("UP");
	}

	/**
	 * 應用程式自己也可以宣告「暫時不接流量」，例如正在預熱快取、正在關閉。
	 * readiness 變成 503，liveness 仍然是 200（不需要重新啟動）。
	 */
	@Test
	void refusingTraffic_shouldMakeReadinessFailButNotLiveness() {
		try {
			AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);

			assertThat(management().get().uri("/actuator/health/readiness").retrieve().toBodilessEntity()
					.getStatusCode().value()).isEqualTo(503);
			assertThat(management().get().uri("/actuator/health/liveness").retrieve().toBodilessEntity()
					.getStatusCode().value()).isEqualTo(200);
		}
		finally {
			AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
		}
	}
}
