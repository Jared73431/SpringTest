package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 啟動完整的 Spring 應用程式（ApplicationContext），確認所有 Bean 都能正常建立。
 */
@SpringBootTest
class SpringHelloWorldApplicationTests {

	// 方法內不需要內容：只要 Context 啟動失敗，測試就會失敗
	@Test
	void contextLoads() {
	}

}
