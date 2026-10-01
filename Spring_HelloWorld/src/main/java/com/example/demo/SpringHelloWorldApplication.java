package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 應用程式進入點。
 * {@code @SpringBootApplication} 包含三個功能：
 * {@code @Configuration}（設定類別）、{@code @EnableAutoConfiguration}（依 classpath 自動設定，例如內嵌 Tomcat）、
 * {@code @ComponentScan}（掃描本 package 及子 package 的 Bean，例如 controller）。
 */
@SpringBootApplication
public class SpringHelloWorldApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringHelloWorldApplication.class, args);
	}

}
