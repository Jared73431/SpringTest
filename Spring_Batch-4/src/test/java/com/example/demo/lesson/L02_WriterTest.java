package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Files;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.price.PriceUpdate;

/**
 * 第 2 課：Writer —— 一次寫入一整個 chunk。
 *
 * <ul>
 * <li>JdbcBatchItemWriter：一個 chunk 的 SQL 用 JDBC batch 一次送出；預設檢查每一句都有更新到資料（assertUpdates）</li>
 * <li>FlatFileItemWriter + @StepScope：每次執行 Step 才建立 Writer，檔名可以用這次執行的資訊</li>
 * </ul>
 */
@SpringBootTest
class L02_WriterTest extends PostgresContainerTestBase {

	@Autowired
	private JdbcBatchItemWriter<PriceUpdate> priceUpdateWriter;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job bookReportJob;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanUp() {
		resetData(dataSource);
	}

	private String price(String isbn) {
		return jdbc.queryForObject("select price from book where isbn = ?", String.class, isbn);
	}

	@Test
	void jdbcBatchItemWriter_shouldWriteTheWholeChunk() throws Exception {
		priceUpdateWriter.write(Chunk.of(new PriceUpdate("9781617297571", new BigDecimal("49.00")),
				new PriceUpdate("9780134685991", new BigDecimal("52.50"))));

		assertThat(price("9781617297571")).isEqualTo("49.00");
		assertThat(price("9780134685991")).isEqualTo("52.50");
	}

	// UPDATE 沒有更新到任何一列（ISBN 不存在）時拋出例外：不會默默略過。
	// 所以 priceUpdateJob 在 Processor 先檢查 ISBN，不存在的跳過並記錄（第 4 課）
	@Test
	void jdbcBatchItemWriter_shouldFail_whenAnUpdateMatchesNoRow() {
		assertThatThrownBy(() -> priceUpdateWriter.write(Chunk.of(new PriceUpdate("9999999999999", BigDecimal.TEN))))
				.isInstanceOf(EmptyResultDataAccessException.class);
	}

	// 修正前：Writer 是 singleton，檔名的時間戳在應用程式啟動時就決定，每次執行都覆蓋同一個檔案
	@Test
	void stepScopedFileWriter_shouldWriteANewFileForEveryExecution() throws Exception {
		var first = jobOperator.startNextInstance(bookReportJob);
		var second = jobOperator.startNextInstance(bookReportJob);

		var firstReport = REPORT_DIRECTORY.resolve("book-report-" + first.getId() + ".csv");
		var secondReport = REPORT_DIRECTORY.resolve("book-report-" + second.getId() + ".csv");
		assertThat(firstReport).exists();
		assertThat(secondReport).exists();
		// BookReportProcessor：書名、出版社轉大寫，依出版年份分類
		assertThat(Files.readAllLines(secondReport))
				.contains("9780201633610,DESIGN PATTERNS,Erich Gamma,1994,ADDISON-WESLEY,65.00,VINTAGE");
	}
}
