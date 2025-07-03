package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.demo.service.OKHttpService;

@SpringBootApplication
public class SpringOkhttpApplication implements CommandLineRunner {

	@Autowired
	private OKHttpService okHttpService;

	public static void main(String[] args) {
		SpringApplication.run(SpringOkhttpApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("=== Spring Boot + OKHttp 練習 ===\n");

		// 執行 GET 請求
		okHttpService.performGetRequest();

		System.out.println("\n" + "=".repeat(50) + "\n");

		// 執行 POST 請求
		okHttpService.performPostRequest();
	}
}
