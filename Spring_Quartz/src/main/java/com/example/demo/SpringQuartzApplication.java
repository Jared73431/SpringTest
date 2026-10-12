package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 修正前加了 @EnableScheduling：那是 @Scheduled 用的，和 Quartz 無關
@SpringBootApplication
public class SpringQuartzApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringQuartzApplication.class, args);
	}
}
