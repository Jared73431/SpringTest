package com.example.demo.lesson;

import static org.springframework.data.relational.core.query.Criteria.where;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;

import com.example.demo.lesson.LessonCategories.Category;

import reactor.test.StepVerifier;

/**
 * 第 5 課：自訂型別轉換（Converter）。
 *
 * <p>
 * 資料庫常用簡短的代碼存放狀態（'A' / 'S'），程式中則希望用 enum。R2DBC 沒有 JPA 的 @Converter / @Enumerated，
 * 改用 Spring Data 的 @WritingConverter / @ReadingConverter，註冊在 R2dbcCustomConversions（見 LessonR2dbcConfiguration）。
 *
 * <p>
 * 沒有自訂 Converter 時，enum 會以名稱（"ACTIVE"）寫入；這裡的欄位是 CHAR(1)，寫入就會失敗。
 */
class L05_ConverterTest extends LessonTestBase {

	@Autowired
	private R2dbcEntityTemplate template;

	@Autowired
	private CategoryRepository categoryRepository;

	@Test
	void writingConverter_shouldStoreEnumAsCode() {
		template.insert(new Category("PERIPHERAL", "周邊設備", CategoryStatus.SUSPENDED)).block();

		// 直接查詢資料表：存的是代碼 'S'，不是 "SUSPENDED"
		StepVerifier.create(databaseClient.sql("SELECT status FROM category WHERE code = 'PERIPHERAL'")
				.map(row -> row.get("status", String.class))
				.one())
				.expectNext("S")
				.verifyComplete();
	}

	@Test
	void readingConverter_shouldMapCodeBackToEnum() {
		databaseClient.sql("INSERT INTO category (code, name, status) VALUES ('CABLE', '線材', 'A')").then().block();

		StepVerifier.create(categoryRepository.findById("CABLE").map(Category::status))
				.expectNext(CategoryStatus.ACTIVE)
				.verifyComplete();
	}

	// 查詢條件中的 enum 也會經過 Converter
	@Test
	void criteria_shouldConvertEnumParameter() {
		template.insert(new Category("PERIPHERAL", "周邊設備", CategoryStatus.ACTIVE)).block();
		template.insert(new Category("LEGACY", "舊型設備", CategoryStatus.SUSPENDED)).block();

		StepVerifier.create(template.select(Category.class)
				.matching(Query.query(where("status").is(CategoryStatus.SUSPENDED)))
				.all()
				.map(Category::code))
				.expectNext("LEGACY")
				.verifyComplete();
	}
}
