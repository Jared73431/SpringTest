package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/**
 * OpenAPI 規格的契約測試：鎖定 /v3/api-docs 的重點內容。
 *
 * <p>
 * API 的文件是給前端、其他團隊或產生 client 用的「契約」。改了 Controller 或 DTO，文件跟著變，
 * 這裡的測試就會失敗，提醒你確認這個變更對使用者的影響（例如欄位改名、必填改變、狀態碼改變）。
 *
 * <p>
 * exportSpec 另外把規格存成 build/openapi/openapi.json 與 openapi.yaml：./gradlew exportOpenApi
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OpenApiContractTest extends PostgresContainerTestBase {

	@Autowired
	private TestRestTemplate rest;

	@LocalServerPort
	private int port;

	private DocumentContext spec;

	@BeforeEach
	void loadSpec() {
		spec = JsonPath.parse(rest.getForObject("/v3/api-docs", String.class));
	}

	// 修正前寫死 http://localhost:8080（應用程式在 8020），Swagger UI 的 Try it out 會打錯 port
	@Test
	void servers_shouldFollowTheActualRequestUrl() {
		assertThat(spec.read("$.servers[*].url", List.class)).containsExactly("http://localhost:" + port);
	}

	@Test
	void paths_shouldContainUserCrud() {
		Map<String, Object> paths = spec.read("$.paths");

		assertThat(paths).containsOnlyKeys("/api/users", "/api/users/{id}");
		assertThat(spec.read("$.paths['/api/users']", Map.class)).containsOnlyKeys("get", "post");
		assertThat(spec.read("$.paths['/api/users/{id}']", Map.class)).containsOnlyKeys("get", "put", "delete");
	}

	// 回傳 ResponseEntity 時 springdoc 推導不出 201，靠 @ApiResponse 明確標註
	@Test
	void createUser_shouldDocument201AndProblemDetailErrors() {
		Map<String, Object> responses = spec.read("$.paths['/api/users'].post.responses");

		assertThat(responses).containsOnlyKeys("201", "400", "409");
		assertThat(spec.read("$.paths['/api/users'].post.responses['400'].content['application/problem+json'].schema['$ref']",
				String.class)).isEqualTo("#/components/schemas/ProblemDetail");
	}

	@Test
	void pathWithId_shouldDocument404() {
		assertThat(spec.read("$.paths['/api/users/{id}'].get.responses", Map.class)).containsOnlyKeys("200", "400", "404");
		assertThat(spec.read("$.paths['/api/users/{id}'].put.responses", Map.class))
				.containsOnlyKeys("200", "400", "404", "409");
		assertThat(spec.read("$.paths['/api/users/{id}'].delete.responses", Map.class)).containsOnlyKeys("204", "400", "404");
	}

	@Test
	void listUsers_shouldHaveNoErrorResponses() {
		assertThat(spec.read("$.paths['/api/users'].get.responses", Map.class)).containsOnlyKeys("200");
	}

	// Bean Validation 自動推導：不需要 @Schema(required = true)、maxLength…
	@Test
	void userRequestSchema_shouldBeInferredFromBeanValidation() {
		String schema = "$.components.schemas.UserRequest";

		assertThat(spec.read(schema + ".required", List.class)).containsExactlyInAnyOrder("username", "name", "email", "age");
		assertThat(spec.read(schema + ".properties.username.maxLength", Integer.class)).isEqualTo(50);
		assertThat(spec.read(schema + ".properties.email.format", String.class)).isEqualTo("email");
		assertThat(spec.read(schema + ".properties.age.minimum", Integer.class)).isZero();
		assertThat(spec.read(schema + ".properties.age.maximum", Integer.class)).isEqualTo(150);
		// @Schema 只補範例值
		assertThat(spec.read(schema + ".properties.username.example", String.class)).isEqualTo("john_doe");
	}

	@Test
	void problemDetailSchema_shouldDescribeFieldErrors() {
		assertThat(spec.read("$.components.schemas.ProblemDetail.properties", Map.class))
				.containsKeys("type", "title", "status", "detail", "instance", "errors");
	}

	// 匯出規格檔：可以交給前端、或用 openapi-generator 產生其他語言的 client
	@Test
	void exportSpec() throws Exception {
		Path dir = Path.of("build", "openapi");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("openapi.json"), rest.getForObject("/v3/api-docs", String.class));
		Files.writeString(dir.resolve("openapi.yaml"), rest.getForObject("/v3/api-docs.yaml", String.class));

		assertThat(dir.resolve("openapi.yaml")).content().contains("/api/users/{id}:");
	}
}
