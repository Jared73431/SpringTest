package com.example.demo.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;

/**
 * Product 的 @Version 樂觀鎖。
 * 測試方法沒有 @Transactional，每次呼叫 Repository 都是獨立的交易，
 * 可以模擬「兩個請求先後讀到同一個版本」的情境。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductOptimisticLockTest {

	@Autowired
	private ProductRepository productRepository;

	@Test
	void save_shouldThrowOptimisticLockingFailure_whenProductWasUpdatedByAnotherTransaction() {
		String id = productRepository
				.save(new Product(UUID.randomUUID().toString(), "Book", new BigDecimal("100.00"), 10)).getId();
		Product first = productRepository.findById(id).orElseThrow();
		Product second = productRepository.findById(id).orElseThrow();

		first.setStock(9);
		productRepository.save(first);
		second.setStock(8);

		assertThatThrownBy(() -> productRepository.save(second))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(productRepository.findById(id).orElseThrow().getStock()).isEqualTo(9);
	}
}
