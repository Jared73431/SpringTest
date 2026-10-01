package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 應用程式進入點。{@code @SpringBootApplication} 會掃描本 package 及子 package 的元件，
 * 並依 classpath 自動設定 DataSource、JPA（Hibernate）、內嵌 Tomcat 等。
 */
@SpringBootApplication
public class SpringJpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringJpaApplication.class, args);
	}

}
