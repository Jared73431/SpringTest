package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.demo.ActuatorTestBase;

/**
 * 第 5 課：把 Actuator 放在另一個 port。
 *
 * <pre>
 * 8115（server.port）           /api/**        對外開放
 * 9115（management.server.port） /actuator/**  只對內開放：Prometheus、K8S 探測、維運人員
 * </pre>
 *
 * 不需要 Spring Security 也能把 Actuator 和對外的 API 分開：防火牆、Docker、K8S 只對外開放 8115。
 * 正式環境通常再加上 Spring Security（例如 /actuator/** 需要 ADMIN），兩者可以一起使用。
 */
class L05_ManagementPortTest extends ActuatorTestBase {

	@Test
	void actuator_shouldNotBeReachableOnTheApplicationPort() {
		assertThat(api().get().uri("/actuator/health").retrieve().toBodilessEntity().getStatusCode().value())
				.isEqualTo(404);
	}

	@Test
	void actuator_shouldBeReachableOnTheManagementPort() {
		assertThat(management().get().uri("/actuator/health").retrieve().toBodilessEntity().getStatusCode().value())
				.isEqualTo(200);
	}

	@Test
	void api_shouldNotBeReachableOnTheManagementPort() {
		assertThat(api().get().uri("/api/books").retrieve().toBodilessEntity().getStatusCode().value()).isEqualTo(200);
		assertThat(management().get().uri("/api/books").retrieve().toBodilessEntity().getStatusCode().value())
				.isEqualTo(404);
	}
}
