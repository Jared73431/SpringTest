package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * /actuator/flyway：以 HTTP 查看每個遷移的版本、描述、類型、狀態與 checksum。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class FlywayActuatorTest extends PostgresContainerTestBase {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void flywayEndpoint_shouldListAppliedMigrations() {
		Map<?, ?> body = restTemplate.getForObject("/actuator/flyway", Map.class);

		Map<?, ?> contexts = (Map<?, ?>) body.get("contexts");
		Map<?, ?> application = (Map<?, ?>) contexts.values().iterator().next();
		Map<?, ?> flywayBeans = (Map<?, ?>) application.get("flywayBeans");
		List<Map> migrations = (List<Map>) ((Map<?, ?>) flywayBeans.get("flyway")).get("migrations");

		assertThat(migrations).extracting(m -> m.get("script")).contains("V1.0.0__create_javastack.sql",
				"db.migration.V1_0_5__ComplexMigration", "R__update_javastack.sql");
		assertThat(migrations).allMatch(m -> "SUCCESS".equals(m.get("state")));
	}
}
