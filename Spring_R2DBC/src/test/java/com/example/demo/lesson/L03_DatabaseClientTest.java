package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

/**
 * 第 3 課：DatabaseClient —— 最低階：自己寫 SQL、自己把每一列轉成物件。
 *
 * <pre>
 * 適合   JOIN、GROUP BY、統計、資料庫特有的語法、回傳結果不是 Entity 的查詢
 * 代價   SQL 與欄位對應都要自己寫，欄位改名時編譯器不會提醒
 * </pre>
 *
 * <pre>
 *                     Repository        R2dbcEntityTemplate     DatabaseClient
 * 程式碼量             最少              中等                    最多
 * 動態條件             ✗                ✓（Criteria）           ✓（自己組 SQL）
 * 結果型別             Entity           Entity                  任意（自己對應）
 * JOIN / GROUP BY     只能寫在 @Query    ✗                      ✓
 * </pre>
 *
 * 三種方式可以在同一個專案中混用：先用 Repository，不夠用時再往下一層。
 */
class L03_DatabaseClientTest extends LessonTestBase {

	record PriceStats(long count, BigDecimal average, BigDecimal max) {
	}

	@Test
	void sql_shouldMapAggregateRowToRecord() {
		var stats = databaseClient.sql("SELECT COUNT(*) AS cnt, AVG(price) AS avg, MAX(price) AS max FROM product")
				.map(row -> new PriceStats(row.get("cnt", Long.class), row.get("avg", BigDecimal.class),
						row.get("max", BigDecimal.class)))
				.one();

		StepVerifier.create(stats)
				.assertNext(s -> {
					assertThat(s.count()).isEqualTo(5);
					assertThat(s.average()).isEqualByComparingTo("1567.70");
					assertThat(s.max()).isEqualByComparingTo("5990.00");
				})
				.verifyComplete();
	}

	// 用 bind 傳入參數：值與 SQL 分開送給資料庫，不會被當成 SQL 的一部分
	@Test
	void bind_shouldPassParametersSafely() {
		StepVerifier.create(databaseClient.sql("SELECT description FROM product WHERE price >= :min ORDER BY price")
				.bind("min", new BigDecimal("1000"))
				.map(row -> row.get("description", String.class))
				.all())
				.expectNext("Keyboard", "Monitor")
				.verifyComplete();
	}

	// ❌ 把使用者輸入直接接進 SQL 字串：輸入 x' OR '1'='1 就變成「條件永遠成立」，查出全部資料（SQL Injection）
	@Test
	void stringConcatenation_shouldBeVulnerableToSqlInjection() {
		String userInput = "x' OR '1'='1";

		StepVerifier.create(databaseClient.sql("SELECT * FROM product WHERE description = '" + userInput + "'")
				.fetch().all())
				.expectNextCount(5) // 本來應該一筆都查不到
				.verifyComplete();

		// ✅ 同樣的輸入用 bind：只會被當成一般字串比對
		StepVerifier.create(databaseClient.sql("SELECT * FROM product WHERE description = :description")
				.bind("description", userInput)
				.fetch().all())
				.verifyComplete();
	}

	// INSERT 之後取得資料庫產生的 id
	@Test
	void insert_shouldReturnGeneratedId() {
		StepVerifier.create(databaseClient.sql("INSERT INTO product (description, price) VALUES (:description, :price)")
				.bind("description", "Webcam")
				.bind("price", new BigDecimal("1590"))
				.filter(statement -> statement.returnGeneratedValues("id"))
				.map(row -> row.get("id", Integer.class))
				.one())
				.expectNext(6)
				.verifyComplete();
	}
}
