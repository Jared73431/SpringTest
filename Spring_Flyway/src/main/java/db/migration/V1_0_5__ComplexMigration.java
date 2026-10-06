package db.migration;

import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Java 遷移：需要依現有資料逐筆計算、SQL 不好表達的資料轉換時使用。
 *
 * <ul>
 * <li>類別名稱就是版本與描述：V1_0_5__ComplexMigration → 版本 1.0.5（Java 類別名稱不能有點，用底線代替）</li>
 * <li>放在 db.migration package，對應 spring.flyway.locations 的 classpath:db/migration</li>
 * <li>與 SQL 遷移在同一個交易中執行，失敗時整個遷移 rollback</li>
 * <li>Java 遷移預設<b>不計算 checksum</b>：修改註解或 log 不會讓驗證失敗（SQL 遷移檔則連一個字都不能改）</li>
 * </ul>
 *
 * 注意：之後的可重複遷移 R__update_javastack.sql 會把 note 全部覆蓋，這個遷移的結果最後看不到，說明見 readme。
 */
public class V1_0_5__ComplexMigration extends BaseJavaMigration {

	private static final Logger log = LoggerFactory.getLogger(V1_0_5__ComplexMigration.class);

	/** 查詢結果（record 取代修正前用 Lombok 的內部類別） */
	private record JavaStackRecord(Long id, String title, String content, String note) {
	}

	@Override
	public void migrate(Context context) {
		// 使用 Flyway 提供的連線（同一個交易）；suppressClose = true：不要在這裡關閉 Flyway 的連線
		JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

		// 1. 查詢所有資料
		List<JavaStackRecord> records = jdbcTemplate.query("SELECT id, title, content, note FROM t_javastack",
				(rs, rowNum) -> new JavaStackRecord(rs.getLong("id"), rs.getString("title"),
						rs.getString("content"), rs.getString("note")));
		log.info("找到 {} 筆資料需要處理", records.size());

		// 2. 依資料內容計算新的 note 並逐筆更新
		for (JavaStackRecord record : records) {
			jdbcTemplate.update("UPDATE t_javastack SET note = ? WHERE id = ?", generateNote(record), record.id());
		}

		// 3. 驗證結果
		Integer specialCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM t_javastack WHERE note LIKE '特殊%'", Integer.class);
		Integer contentCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM t_javastack WHERE note = '包含內容關鍵字'", Integer.class);
		log.info("資料遷移完成：特殊標題 {} 筆、包含內容關鍵字 {} 筆", specialCount, contentCount);
	}

	// 比對的字串（標題1、內容）必須與 V1.0.1 新增的資料一致
	private static String generateNote(JavaStackRecord record) {
		if (record.title().contains("標題1")) {
			return "特殊標題1";
		} else if (record.content().contains("內容")) {
			return "包含內容關鍵字";
		}
		return "預設備註";
	}
}
