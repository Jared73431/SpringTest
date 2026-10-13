package com.example.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 整合測試共用：PostgreSQL container（不會連到本機的資料庫），以及兩個 port 的 HTTP client。
 *
 * <pre>
 * api()          應用程式的 port（/api/books）
 * management()   Actuator 的 port（/actuator/**）
 * </pre>
 *
 * 兩個都用隨機 port；回應 4xx / 5xx 時不拋出例外，讓測試直接檢查狀態碼。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
public abstract class ActuatorTestBase {

	@ServiceConnection
	protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static {
		POSTGRES.start();
	}

	@LocalServerPort
	private int serverPort;

	@LocalManagementPort
	private int managementPort;

	protected RestClient api() {
		return client(serverPort);
	}

	protected RestClient management() {
		return client(managementPort);
	}

	private static RestClient client(int port) {
		return RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.defaultStatusHandler(status -> true, (request, response) -> {
				})
				.build();
	}
}
