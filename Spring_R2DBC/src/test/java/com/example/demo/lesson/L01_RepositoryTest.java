package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.demo.entity.Product;

import reactor.test.StepVerifier;

/**
 * 第 1 課：ReactiveCrudRepository —— 最高階、寫最少程式碼的方式。
 *
 * <pre>
 * 適合   固定條件的查詢：findById、findByXxx、簡單的 @Query
 * 不適合 條件會依參數變化的動態查詢（方法名稱是寫死的）→ 第 2 課
 * </pre>
 *
 * 和 Spring Data JPA 的 Repository 寫法幾乎一樣，差別在回傳 Mono / Flux，而且沒有關聯、lazy loading、Page。
 */
class L01_RepositoryTest extends LessonTestBase {

	@Autowired
	private ProductQueryRepository repository;

	@Test
	void derivedQuery_shouldBuildSqlFromMethodName() {
		StepVerifier.create(repository.findByDescriptionContainingIgnoreCase("mouse").map(Product::getDescription))
				.expectNext("Mouse", "Mouse Pad")
				.verifyComplete();
	}

	@Test
	void derivedQuery_shouldSupportRangeAndOrdering() {
		StepVerifier.create(repository.findByPriceBetweenOrderByPriceAsc(new BigDecimal("100"), new BigDecimal("2000"))
				.map(Product::getDescription))
				.expectNext("Mouse Pad", "Mouse", "Keyboard")
				.verifyComplete();
	}

	// 分頁：第 2 頁、每頁 2 筆、依價格排序。R2DBC 沒有 Page（不會自動 count），需要總筆數時另外呼叫 count()
	@Test
	void pageable_shouldLimitAndOffset() {
		var secondPage = PageRequest.of(1, 2, Sort.by("price"));

		StepVerifier.create(repository.findAllBy(secondPage).map(Product::getDescription))
				.expectNext("Mouse", "Keyboard")
				.verifyComplete();
		StepVerifier.create(repository.count()).expectNext(5L).verifyComplete();
	}

	@Test
	void query_shouldBindNamedParameters() {
		StepVerifier.create(repository.findCheaperThan(new BigDecimal("300")).map(Product::getDescription))
				.expectNext("Mouse Pad", "USB Cable")
				.verifyComplete();
	}

	@Test
	void modifyingQuery_shouldReturnAffectedRowCount() {
		StepVerifier.create(repository.raisePrice(new BigDecimal("1.1"), new BigDecimal("1000")))
				.expectNext(2) // Keyboard、Monitor
				.verifyComplete();

		StepVerifier.create(repository.findById(1).map(Product::getPrice))
				.assertNext(price -> assertThat(price).isEqualByComparingTo("1320.55"))
				.verifyComplete();
	}
}
