package com.example.demo.price;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 檢查 ISBN 是否存在。不存在時拋出 UnknownIsbnException，由 Step 的 skip 設定跳過。
 *
 * <p>
 * 每一筆查一次資料庫：資料量大時可以改成先把所有 ISBN 讀進記憶體，或在 Writer 檢查更新的筆數。
 */
public class PriceUpdateProcessor implements ItemProcessor<PriceUpdate, PriceUpdate> {

	private final JdbcTemplate jdbcTemplate;

	public PriceUpdateProcessor(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public PriceUpdate process(PriceUpdate update) {
		Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM book WHERE isbn = ?)",
				Boolean.class, update.isbn());
		if (!Boolean.TRUE.equals(exists)) {
			throw new UnknownIsbnException(update.isbn());
		}
		return update;
	}
}
