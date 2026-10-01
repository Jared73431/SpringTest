package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 啟動類別。@SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan，
 * 會掃描 com.example.demo 以下的所有元件，並依 classpath 自動設定 DataSource、JPA、Web MVC 等。
 */
@SpringBootApplication
public class SpringJpa2Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringJpa2Application.class, args);
	}

}
