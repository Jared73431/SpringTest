package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringRedisApplication.class, args);

		System.out.println("=================================");
		System.out.println("Redis Practice Application Started!");
		System.out.println("Server running on: http://localhost:8015");
		System.out.println("API 測試端點:");
		System.out.println("- POST /redis/init-test-data (初始化測試數據)");
		System.out.println("- GET  /redis/string/{key}");
		System.out.println("- POST /redis/string/{key}");
		System.out.println("- GET  /redis/user/{id}");
		System.out.println("- POST /redis/user/{id}");
		System.out.println("更多 API 請查看 RedisController");
		System.out.println("=================================");
	}

}
