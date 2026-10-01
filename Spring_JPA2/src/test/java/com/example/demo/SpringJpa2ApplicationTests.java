package com.example.demo;

import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 確認完整的 Spring 應用程式（含資料庫連線）能正常啟動。
 * 透過 TestcontainersConfiguration 連線到臨時的 PostgreSQL 容器，不會碰到本機開發用的資料庫。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SpringJpa2ApplicationTests {

	@Test
	void contextLoads() {
	}

}
