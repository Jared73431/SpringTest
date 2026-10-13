package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.example.demo.ActuatorTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 3 課：Info 與 Metrics。
 *
 * <pre>
 * /actuator/info                         版本（build-info）、Java、OS、info.* 設定
 * /actuator/metrics                      所有 metric 的名稱
 * /actuator/metrics/{name}?tag=key:value 某個 metric 的數值，可以用 tag 篩選
 * /actuator/prometheus                   全部 metrics，Prometheus 的文字格式（給 Prometheus 抓）
 * </pre>
 */
class L03_InfoAndMetricsTest extends ActuatorTestBase {

	private double metricValue(String name, String statistic, String... tags) {
		// tag 的值可能有 {id} 這類字元，用 URI 變數傳入，避免被當成樣板展開
		var uri = new StringBuilder("/actuator/metrics/" + name + "?");
		for (int i = 0; i < tags.length; i++) {
			uri.append("tag={t").append(i).append("}&");
		}
		var response = management().get().uri(uri.toString(), (Object[]) tags).retrieve().toEntity(String.class);
		if (response.getStatusCode().value() == 404) {
			return 0;
		}
		List<Double> values = JsonPath.read(response.getBody(),
				"$.measurements[?(@.statistic == '" + statistic + "')].value");
		return values.getFirst();
	}

	// build.gradle 的 springBoot { buildInfo() } 產生 build-info.properties
	@Test
	void info_shouldShowBuildJavaAndCustomInformation() {
		String body = management().get().uri("/actuator/info").retrieve().body(String.class);

		assertThat((String) JsonPath.read(body, "$.build.artifact")).isEqualTo("Spring_Actuator");
		assertThat((String) JsonPath.read(body, "$.build.version")).isEqualTo("0.0.1-SNAPSHOT");
		assertThat((String) JsonPath.read(body, "$.java.version")).startsWith("21");
		assertThat((String) JsonPath.read(body, "$.app.description")).isEqualTo("Spring Boot Actuator practice");
	}

	// 自訂的 Counter：每建立一本書加 1
	@Test
	void customCounter_shouldCountCreatedBooks() {
		double before = metricValue("bookstore.books.added", "COUNT");

		api().post().uri("/api/books").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"title":"Clean Code","author":"Robert C. Martin"}
						""")
				.retrieve().toBodilessEntity();

		assertThat(metricValue("bookstore.books.added", "COUNT")).isEqualTo(before + 1);
	}

	// HTTP 請求自動記錄在 http.server.requests：次數、總時間、最長時間，可以用 uri、status 等 tag 篩選
	@Test
	void httpRequests_shouldBeRecordedAutomatically() {
		double before = metricValue("http.server.requests", "COUNT", "uri:/api/books/{id}", "status:404");

		api().get().uri("/api/books/999999").retrieve().toBodilessEntity();

		assertThat(metricValue("http.server.requests", "COUNT", "uri:/api/books/{id}", "status:404"))
				.isEqualTo(before + 1);
	}

	// 自訂的 Timer：搜尋的次數與時間
	@Test
	void customTimer_shouldRecordSearches() {
		double before = metricValue("bookstore.books.search", "COUNT");

		api().get().uri("/api/books?title=java").retrieve().toBodilessEntity();

		assertThat(metricValue("bookstore.books.search", "COUNT")).isEqualTo(before + 1);
		assertThat(metricValue("bookstore.books.search", "TOTAL_TIME")).isPositive();
	}

	// Prometheus 格式：名稱的 . 換成 _，Counter 加上 _total；每個 metric 都有 application 標籤
	@Test
	void prometheus_shouldExposeMetricsInTheTextFormat() {
		api().get().uri("/api/books").retrieve().toBodilessEntity();

		String body = management().get().uri("/actuator/prometheus").retrieve().body(String.class);

		assertThat(body).contains("bookstore_books_added_total{application=\"Spring_Actuator\"")
				.contains("http_server_requests_seconds_bucket{")
				.contains("jvm_memory_used_bytes{");
	}
}
