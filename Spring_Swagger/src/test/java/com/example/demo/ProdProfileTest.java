package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * prod profile（application-prod.yml）：OpenAPI 文件與 Swagger UI 都關閉，API 本身照常運作。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("prod")
class ProdProfileTest extends PostgresContainerTestBase {

	@Autowired
	private TestRestTemplate rest;

	@Test
	void apiDocsAndSwaggerUi_shouldBeDisabled() {
		assertThat(rest.getForEntity("/v3/api-docs", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(rest.getForEntity("/swagger-ui.html", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void api_shouldStillWork() {
		assertThat(rest.getForEntity("/api/users", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
