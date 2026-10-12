package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 啟動時執行 importPersonJob，做完就結束。
 */
@SpringBootApplication
public class SpringBatch3Application {

	// exit code：Job COMPLETED 為 0，失敗不為 0（見 Spring_Batch）
	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(SpringBatch3Application.class, args)));
	}
}
