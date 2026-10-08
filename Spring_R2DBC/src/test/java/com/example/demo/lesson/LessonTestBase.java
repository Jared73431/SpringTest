package com.example.demo.lesson;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepo;

import reactor.core.publisher.Flux;

/**
 * 課程共用：每個測試前清空資料表，放入同樣的 5 筆商品。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class LessonTestBase extends PostgresContainerTestBase {

	@Autowired
	protected DatabaseClient databaseClient;

	@Autowired
	protected ProductRepo productRepo;

	@BeforeEach
	void resetProducts() {
		databaseClient.sql("TRUNCATE product, category RESTART IDENTITY CASCADE").then()
				.thenMany(productRepo.saveAll(Flux.just(
						new Product("Keyboard", new BigDecimal("1200.50")),
						new Product("Mouse", new BigDecimal("350.00")),
						new Product("Monitor", new BigDecimal("5990.00")),
						new Product("USB Cable", new BigDecimal("99.00")),
						new Product("Mouse Pad", new BigDecimal("199.00")))))
				.blockLast();
	}
}
