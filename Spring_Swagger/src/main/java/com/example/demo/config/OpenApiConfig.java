package com.example.demo.config;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * OpenAPI 文件的全域設定。
 *
 * <p>
 * 修正前寫死了 servers（http://localhost:8080 與 https://api.example.com），但應用程式跑在 8020，
 * Swagger UI 的「Try it out」會打到錯的 port。不設定 servers 時，springdoc 會使用目前請求的網址，在任何環境都正確。
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	private static final String PROBLEM_DETAIL = "ProblemDetail";

	@Bean
	OpenAPI userApi() {
		return new OpenAPI()
				.info(new Info()
						.title("使用者管理 API")
						.description("Spring Boot + springdoc-openapi 練習專案：錯誤回應一律使用 RFC 9457 ProblemDetail")
						.version("v1")
						.license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
	}

	/**
	 * 錯誤回應文件化：依照「路徑有沒有 {id}」、「是不是 POST / PUT」自動加上 400 / 404 / 409，
	 * 不必在每個 Controller 方法上重複寫 @ApiResponses（修正前每個方法各寫一份，而且寫的狀態碼和實際不符）。
	 */
	@Bean
	OpenApiCustomizer problemDetailResponses() {
		// schema 要在這裡加：springdoc 掃描 Controller 後會重建 components，寫在 OpenAPI bean 裡的 schema 會被蓋掉
		return openApi -> {
			openApi.getComponents().addSchemas(PROBLEM_DETAIL, problemDetailSchema());
			addErrorResponses(openApi);
		};
	}

	private static void addErrorResponses(OpenAPI openApi) {
		openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
			boolean hasPathVariable = path.contains("{");
			boolean hasBody = method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT;
			if (hasPathVariable || hasBody) {
				addProblem(op, "400", hasBody ? "驗證失敗（errors 列出每個欄位的錯誤）或格式錯誤" : "參數格式錯誤（例如 id 不是數字）");
			}
			if (hasPathVariable) {
				addProblem(op, "404", "找不到資源");
			}
			if (hasBody) {
				addProblem(op, "409", "帳號或電子郵件已被使用");
			}
		}));
	}

	private static void addProblem(Operation operation, String status, String description) {
		Schema<?> ref = new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL);
		operation.getResponses().addApiResponse(status, new ApiResponse()
				.description(description)
				.content(new Content().addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						new io.swagger.v3.oas.models.media.MediaType().schema(ref))));
	}

	// Spring 的 ProblemDetail 類別有 getProperties()，直接交給 springdoc 推導會多出一個 properties 欄位，所以手動描述實際的 JSON
	private static Schema<?> problemDetailSchema() {
		return new ObjectSchema()
				.description("RFC 9457 Problem Details")
				.addProperty("type", new StringSchema().example("about:blank"))
				.addProperty("title", new StringSchema().example("Not Found"))
				.addProperty("status", new IntegerSchema().example(404))
				.addProperty("detail", new StringSchema().example("找不到使用者：1"))
				.addProperty("instance", new StringSchema().example("/api/users/1"))
				.addProperty("errors", new ObjectSchema()
						.description("只有驗證失敗時才有：欄位名稱 → 錯誤訊息")
						.additionalProperties(new StringSchema())
						.example(Map.of("email", "電子郵件格式不正確")));
	}
}
