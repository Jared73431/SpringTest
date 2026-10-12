package com.example.demo.order;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.common.TimestampIncrementer;

/**
 * orderShippingJob：把待出貨（PENDING）的訂單改成已出貨（SHIPPED）。
 *
 * <p>
 * <b>狀態欄位 + 時間戳</b>（第 6 課）：
 * <ul>
 * <li>Reader 只讀 status = 'PENDING'，Writer 更新時一併改掉 status：處理過的訂單下一次就不會再被讀到</li>
 * <li>所以這個 Job 是冪等的，用 timestamp 參數讓每次都是新的 JobInstance，隨時都能再執行</li>
 * <li>中途失敗也沒關係：已經 commit 的 chunk 狀態已經改了，下一次只會處理剩下的訂單</li>
 * </ul>
 *
 * Reader 一定要用「依 sortKey 往後查」的 JdbcPagingItemReader：用 OFFSET 分頁的 Reader 在更新篩選欄位時會漏掉資料（第 6 課）。
 */
@Configuration
public class OrderShippingJobConfig {

	/** 刻意設得比訂單數小，讓分頁的效果看得到 */
	static final int PAGE_SIZE = 3;

	@Bean
	public JdbcPagingItemReader<BookOrder> pendingOrderReader(DataSource dataSource) throws Exception {
		return new JdbcPagingItemReaderBuilder<BookOrder>()
				.name("pendingOrderReader")
				.dataSource(dataSource)
				.selectClause("SELECT id, book_id, quantity, status")
				.fromClause("FROM book_order")
				.whereClause("WHERE status = 'PENDING'")
				.sortKeys(Map.of("id", Order.ASCENDING))
				.pageSize(PAGE_SIZE)
				.rowMapper(new DataClassRowMapper<>(BookOrder.class))
				// 狀態本身就記錄了處理進度：重新執行時從資料庫的狀態判斷，不需要 Reader 記住讀到哪裡
				.saveState(false)
				.build();
	}

	// WHERE 也檢查 status：就算兩個執行同時處理同一筆，也只會更新一次。
	// 這時另一邊更新 0 筆是正常的，所以關掉 assertUpdates（預設更新 0 筆時拋出 EmptyResultDataAccessException）
	@Bean
	public JdbcBatchItemWriter<BookOrder> shipOrderWriter(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<BookOrder>()
				.dataSource(dataSource)
				.sql("UPDATE book_order SET status = 'SHIPPED', shipped_at = now() WHERE id = :id AND status = 'PENDING'")
				.beanMapped()
				.assertUpdates(false)
				.build();
	}

	// 沒有商業邏輯，不需要 Processor
	@Bean
	public Step orderShippingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JdbcPagingItemReader<BookOrder> pendingOrderReader, JdbcBatchItemWriter<BookOrder> shipOrderWriter) {
		return new StepBuilder("orderShippingStep", jobRepository)
				.<BookOrder, BookOrder>chunk(PAGE_SIZE)
				.transactionManager(transactionManager)
				.reader(pendingOrderReader)
				.writer(shipOrderWriter)
				.build();
	}

	@Bean
	public Job orderShippingJob(JobRepository jobRepository, Step orderShippingStep) {
		return new JobBuilder("orderShippingJob", jobRepository)
				.incrementer(new TimestampIncrementer())
				.start(orderShippingStep)
				.build();
	}
}
