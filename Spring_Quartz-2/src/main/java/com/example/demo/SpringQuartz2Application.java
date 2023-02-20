package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SpringQuartz2Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringQuartz2Application.class, args);
	}

}
