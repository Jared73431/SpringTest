package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 確認完整的 Spring 應用程式（含資料庫連線）能正常啟動。
 * 繼承 PostgresContainerTestBase，因此連線到 Testcontainers 的臨時資料庫，而不是本機開發用的資料庫。
 */
@SpringBootTest
class SpringFeignServerApplicationTests extends PostgresContainerTestBase {

	@Test
	void contextLoads() {
	}

}
