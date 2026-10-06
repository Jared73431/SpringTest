package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redis 練習：五種資料結構的操作，以及排行榜、限流、分散式鎖等實務情境。
 * 啟動後可呼叫 POST /api/redis/sample-data 建立範例資料，API 清單見 readme。
 */
@SpringBootApplication
public class SpringRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringRedisApplication.class, args);
	}

}
