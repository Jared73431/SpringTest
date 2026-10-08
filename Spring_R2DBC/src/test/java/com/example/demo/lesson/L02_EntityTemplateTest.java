package com.example.demo.lesson;

import static org.springframework.data.relational.core.query.Criteria.where;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;

import com.example.demo.entity.Product;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * 第 2 課：R2dbcEntityTemplate + Criteria —— 用程式組合查詢條件，結果仍然對應到 Entity。
 *
 * <pre>
 * 適合   動態查詢：搜尋畫面上「有填的條件才加進 WHERE」
 * 不適合 JOIN、GROUP BY 等複雜 SQL → 第 3 課
 * </pre>
 *
 * Spring_R2DBC2 的搜尋功能修正前只看第一個參數（if / else 依序判斷），就是這一課要解決的問題。
 */
class L02_EntityTemplateTest extends LessonTestBase {

	@Autowired
	private R2dbcEntityTemplate template;

	/** 動態條件：參數是 null 就不加進條件，全部都是 null 時回傳全部資料 */
	private Flux<Product> search(String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
		Criteria criteria = Criteria.empty();
		if (keyword != null) {
			criteria = criteria.and(where("description").like("%" + keyword + "%").ignoreCase(true));
		}
		if (minPrice != null) {
			criteria = criteria.and(where("price").greaterThanOrEquals(minPrice));
		}
		if (maxPrice != null) {
			criteria = criteria.and(where("price").lessThanOrEquals(maxPrice));
		}
		return template.select(Product.class)
				.matching(Query.query(criteria).sort(Sort.by("price")))
				.all();
	}

	@Test
	void search_shouldCombineAllGivenConditions() {
		StepVerifier.create(search("mouse", new BigDecimal("300"), null).map(Product::getDescription))
				.expectNext("Mouse")
				.verifyComplete();
	}

	@Test
	void search_shouldUseOnlyMinPrice_whenMaxPriceIsMissing() {
		StepVerifier.create(search(null, new BigDecimal("1000"), null).map(Product::getDescription))
				.expectNext("Keyboard", "Monitor")
				.verifyComplete();
	}

	@Test
	void search_shouldReturnAll_whenNoConditionIsGiven() {
		StepVerifier.create(search(null, null, null)).expectNextCount(5).verifyComplete();
	}

	// 只取一筆、限制筆數
	@Test
	void select_shouldSupportFirstAndLimit() {
		StepVerifier.create(template.select(Product.class)
				.matching(Query.empty().sort(Sort.by(Sort.Direction.DESC, "price")).limit(1))
				.first()
				.map(Product::getDescription))
				.expectNext("Monitor")
				.verifyComplete();
	}

	// 批次更新：不必先查出來再一筆筆 save，回傳受影響的筆數
	@Test
	void update_shouldModifyMatchingRows() {
		StepVerifier.create(template.update(Product.class)
				.matching(Query.query(where("description").like("Mouse%")))
				.apply(Update.update("price", new BigDecimal("1"))))
				.expectNext(2L)
				.verifyComplete();
	}

	@Test
	void delete_shouldRemoveMatchingRows() {
		StepVerifier.create(template.delete(Product.class)
				.matching(Query.query(where("price").lessThan(new BigDecimal("200"))))
				.all())
				.expectNext(2L) // USB Cable、Mouse Pad
				.verifyComplete();
	}
}
