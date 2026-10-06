package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 應用程式啟動時，Flyway 對空資料庫執行所有遷移；驗證執行紀錄、資料表結構與最終資料。
 */
@SpringBootTest
class MigrationBaselineTest extends PostgresContainerTestBase {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// 版本遷移依版本號執行；可重複遷移（version 為 null）在最後，彼此依描述的字母順序
	@Test
	void history_shouldContainAllMigrationsInOrder() {
		List<Map<String, Object>> history = jdbcTemplate.queryForList(
				"SELECT version, description, type, success FROM flyway_schema_history ORDER BY installed_rank");

		assertThat(history).extracting(row -> row.get("version")).containsExactly("1.0.0", "1.0.1", "1.0.2",
				"1.0.3", "1.0.4", "1.0.5", null, null);
		assertThat(history).extracting(row -> row.get("type")).containsExactly("SQL", "SQL", "SQL", "SQL", "SQL",
				"JDBC", "SQL", "SQL");
		assertThat(history.subList(6, 8)).extracting(row -> row.get("description"))
				.containsExactly("javastack summary view", "update javastack");
		assertThat(history).allMatch(row -> Boolean.TRUE.equals(row.get("success")));
	}

	@Test
	void table_shouldHaveAllColumnsAndRows() {
		List<String> columns = jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns WHERE table_name = 't_javastack' ORDER BY ordinal_position",
				String.class);

		assertThat(columns).containsExactly("id", "title", "content", "note", "time");
		assertThat(jdbcTemplate.queryForList("SELECT title FROM t_javastack ORDER BY id", String.class))
				.containsExactly("flyway:標題1", "flyway:標題2", "flyway:標題3", "flyway:標題4", "flyway:標題5");
	}

	// 可重複遷移（R__update_javastack）在所有版本遷移之後執行，把 V1_0_5（Java）計算出的 note 全部覆蓋
	@Test
	void repeatableMigration_shouldOverwriteJavaMigrationResult() {
		assertThat(jdbcTemplate.queryForList("SELECT DISTINCT note FROM t_javastack", String.class))
				.containsExactly("flyway repeated ok5");
	}

	// 可重複遷移的正確用法：CREATE OR REPLACE VIEW
	@Test
	void summaryView_shouldAggregateByNote() {
		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT note, total FROM v_javastack_summary");

		assertThat(row.get("note")).isEqualTo("flyway repeated ok5");
		assertThat(((Number) row.get("total")).intValue()).isEqualTo(5);
	}
}
