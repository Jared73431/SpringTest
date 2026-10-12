package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 啟動時執行 helloJob，做完就結束。命令列的 key=value 參數會成為 JobParameters：
 *
 * <pre>
 * java -jar app.jar date=2026-10-12
 * </pre>
 */
@SpringBootApplication
public class SpringBatch2Application {

	// exit code：Job COMPLETED 為 0，失敗不為 0（見 Spring_Batch）
	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(SpringBatch2Application.class, args)));
	}

}
