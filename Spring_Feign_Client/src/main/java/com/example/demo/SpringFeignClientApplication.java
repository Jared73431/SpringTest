package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class SpringFeignClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringFeignClientApplication.class, args);
	}

}
