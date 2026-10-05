package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 確認完整的 Spring 應用程式（含資料庫連線）能正常啟動。
 * 繼承 PostgresContainerTestBase：使用 Testcontainers 的臨時資料庫，並關閉 Eureka 註冊。
 */
@SpringBootTest
class SpringEurekaProviderApplicationTests extends PostgresContainerTestBase {

	@Test
	void contextLoads() {
	}

}
