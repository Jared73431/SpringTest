package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Baseline：應用程式啟動時，Flyway 對空資料庫執行所有遷移，鎖定目前的結果。
 */
@SpringBootTest
class MigrationBaselineTest extends PostgresContainerTestBase {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void history_shouldContainAllMigrationsInOrder() {
		List<Map<String, Object>> history = jdbcTemplate.queryForList(
				"SELECT version, type, success FROM flyway_schema_history ORDER BY installed_rank");

		assertThat(history).extracting(row -> row.get("version")).containsExactly("1.0.0", "1.0.1", "1.0.2",
				"1.0.3", "1.0.4", "1.0.5", null);
		assertThat(history).extracting(row -> row.get("type")).containsExactly("SQL", "SQL", "SQL", "SQL", "SQL",
				"JDBC", "SQL");
		assertThat(history).allMatch(row -> Boolean.TRUE.equals(row.get("success")));
	}

	@Test
	void table_shouldHaveAllColumnsAndRows() {
		List<String> columns = jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns WHERE table_name = 't_javastack' ORDER BY ordinal_position",
				String.class);

		assertThat(columns).containsExactly("id", "title", "content", "note", "time");
		assertThat(jdbcTemplate.queryForList("SELECT title FROM t_javastack ORDER BY id", String.class))
				.containsExactly("flyway:标题1", "flyway:标题2", "flyway:标题3", "flyway:标题4", "flyway:标题5");
	}

	// [Potential Bug] 可重複遷移（R__）在所有版本遷移之後執行，把 V1_0_5（Java）計算出的 note 全部覆蓋掉
	@Test
	void repeatableMigration_shouldOverwriteJavaMigrationResult() {
		assertThat(jdbcTemplate.queryForList("SELECT DISTINCT note FROM t_javastack", String.class))
				.containsExactly("flyway repeated ok5");
	}
}
