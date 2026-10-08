package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.mapping.Table;

import com.example.demo.lesson.LessonCategories.Category;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 第 7 課：R2DBC 沒有關聯對應。
 *
 * <p>
 * 直覺以為：像 JPA 一樣在 Product 上寫 @ManyToOne Category category，取用時自動查詢。<br>
 * 實際上：R2DBC 沒有 @OneToMany / @ManyToOne、沒有 lazy loading。Entity 只存外鍵（categoryCode），
 * 要另外查詢或自己寫 JOIN。好處是每一次查詢都是明確寫出來的，不會在不知不覺中產生 N+1。
 *
 * <pre>
 * 做法                               查詢次數（5 個商品）
 * 每個商品 flatMap 查一次分類          1 + 5（N+1）
 * 收集代碼後一次查詢所有分類（批次）     1 + 1
 * DatabaseClient 寫 JOIN               1
 * </pre>
 */
class L07_RelationshipTest extends LessonTestBase {

	/** 課程用：同一張 product 表，多對應 category_code 欄位（只有外鍵，沒有 Category 物件） */
	@Table("product")
	record ProductRow(@Id Integer id, String description, BigDecimal price, String categoryCode) {
	}

	record ProductWithCategory(String description, String categoryName) {
	}

	@Autowired
	private R2dbcEntityTemplate template;

	@Autowired
	private CategoryRepository categoryRepository;

	private final AtomicInteger categoryQueries = new AtomicInteger();

	@BeforeEach
	void assignCategories() {
		Flux.just(new Category("PERIPHERAL", "周邊設備", CategoryStatus.ACTIVE),
				new Category("DISPLAY", "顯示器", CategoryStatus.ACTIVE),
				new Category("CABLE", "線材", CategoryStatus.ACTIVE))
				.concatMap(template::insert)
				.thenMany(databaseClient.sql("""
						UPDATE product SET category_code = CASE
						    WHEN description = 'Monitor' THEN 'DISPLAY'
						    WHEN description = 'USB Cable' THEN 'CABLE'
						    ELSE 'PERIPHERAL' END
						""").fetch().rowsUpdated())
				.blockLast();
	}

	private Flux<ProductRow> products() {
		return template.select(ProductRow.class).all().sort((a, b) -> a.id().compareTo(b.id()));
	}

	/** 查詢分類，並計算查詢次數 */
	private Mono<Category> findCategory(String code) {
		return categoryRepository.findById(code).doOnSubscribe(s -> categoryQueries.incrementAndGet());
	}

	@Test
	void entity_shouldHoldOnlyForeignKey() {
		StepVerifier.create(products().map(ProductRow::categoryCode))
				.expectNext("PERIPHERAL", "PERIPHERAL", "DISPLAY", "CABLE", "PERIPHERAL")
				.verifyComplete();
	}

	// ❌ N+1：每個商品各查一次分類（JPA 的 lazy loading 也常不知不覺變成這樣）
	@Test
	void flatMapPerRow_shouldQueryCategoryOncePerProduct() {
		Flux<ProductWithCategory> result = products()
				.concatMap(p -> findCategory(p.categoryCode()).map(c -> new ProductWithCategory(p.description(), c.name())));

		StepVerifier.create(result.map(ProductWithCategory::categoryName))
				.expectNext("周邊設備", "周邊設備", "顯示器", "線材", "周邊設備")
				.verifyComplete();
		assertThat(categoryQueries).hasValue(5);
	}

	// ✅ 批次：先收集所有商品，取出不重複的分類代碼，一次查完，再在記憶體中組合
	@Test
	void batchLoading_shouldQueryCategoriesOnce() {
		Flux<ProductWithCategory> result = products().collectList().flatMapMany(products -> {
			List<String> codes = products.stream().map(ProductRow::categoryCode).distinct().toList();
			Mono<Map<String, String>> namesByCode = categoryRepository.findAllById(codes)
					.doOnSubscribe(s -> categoryQueries.incrementAndGet())
					.collectMap(Category::code, Category::name);
			return namesByCode.flatMapMany(names -> Flux.fromIterable(products)
					.map(p -> new ProductWithCategory(p.description(), names.get(p.categoryCode()))));
		});

		StepVerifier.create(result.map(ProductWithCategory::categoryName))
				.expectNext("周邊設備", "周邊設備", "顯示器", "線材", "周邊設備")
				.verifyComplete();
		assertThat(categoryQueries).hasValue(1);
	}

	// ✅ JOIN：一次查詢，結果直接對應到 record（第 3 課的 DatabaseClient）
	@Test
	void join_shouldLoadProductsWithCategoryInOneQuery() {
		Flux<ProductWithCategory> result = databaseClient.sql("""
				SELECT p.description, c.name AS category_name
				FROM product p JOIN category c ON c.code = p.category_code
				ORDER BY p.id
				""")
				.map(row -> new ProductWithCategory(row.get("description", String.class),
						row.get("category_name", String.class)))
				.all();

		StepVerifier.create(result)
				.expectNext(new ProductWithCategory("Keyboard", "周邊設備"),
						new ProductWithCategory("Mouse", "周邊設備"),
						new ProductWithCategory("Monitor", "顯示器"),
						new ProductWithCategory("USB Cable", "線材"),
						new ProductWithCategory("Mouse Pad", "周邊設備"))
				.verifyComplete();
	}
}
