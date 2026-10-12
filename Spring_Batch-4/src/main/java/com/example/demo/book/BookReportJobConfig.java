package com.example.demo.book;

import java.nio.file.Path;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.common.TimestampIncrementer;

/**
 * bookReportJob：讀取所有書籍 → 分類 → 寫成 CSV 報表。
 *
 * <pre>
 * Reader：JdbcPagingItemReader（每次查一頁，依 id 排序）
 * Processor：BookReportProcessor
 * Writer：FlatFileItemWriter（@StepScope：每次執行一個新檔案 book-report-{jobExecutionId}.csv）
 * </pre>
 *
 * 報表只讀不寫，重做幾次都一樣，所以用 timestamp 參數讓每次都能執行。
 */
@Configuration
public class BookReportJobConfig {

	@Bean
	public JdbcPagingItemReader<Book> bookReader(DataSource dataSource) throws Exception {
		return new JdbcPagingItemReaderBuilder<Book>()
				.name("bookReader")
				.dataSource(dataSource)
				.selectClause("SELECT id, isbn, title, author, published_year, publisher, price")
				.fromClause("FROM book")
				.sortKeys(Map.of("id", Order.ASCENDING))
				.pageSize(100)
				.rowMapper(new DataClassRowMapper<>(Book.class))
				.build();
	}

	@Bean
	public BookReportProcessor bookReportProcessor() {
		return new BookReportProcessor();
	}

	// 修正前：singleton 的 Writer，檔名的時間戳在「應用程式啟動時」就決定，每次執行都覆蓋同一個檔案。
	// @StepScope：每次執行 Step 才建立，可以使用這次執行的資訊（jobExecutionId、JobParameters）
	@Bean
	@StepScope
	public FlatFileItemWriter<BookReportLine> bookReportWriter(
			@Value("#{stepExecution.jobExecutionId}") Long jobExecutionId,
			@Value("${app.report.directory}") String directory) {
		return new FlatFileItemWriterBuilder<BookReportLine>()
				.name("bookReportWriter")
				.resource(new FileSystemResource(Path.of(directory, "book-report-" + jobExecutionId + ".csv")))
				.headerCallback(writer -> writer.write("isbn,title,author,publishedYear,publisher,price,category"))
				.delimited()
				.sourceType(BookReportLine.class)
				.names("isbn", "title", "author", "publishedYear", "publisher", "price", "category")
				.build();
	}

	@Bean
	public Step bookReportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JdbcPagingItemReader<Book> bookReader, BookReportProcessor bookReportProcessor,
			FlatFileItemWriter<BookReportLine> bookReportWriter) {
		return new StepBuilder("bookReportStep", jobRepository)
				.<Book, BookReportLine>chunk(100)
				.transactionManager(transactionManager)
				.reader(bookReader)
				.processor(bookReportProcessor)
				.writer(bookReportWriter)
				.build();
	}

	@Bean
	public Job bookReportJob(JobRepository jobRepository, Step bookReportStep) {
		return new JobBuilder("bookReportJob", jobRepository)
				.incrementer(new TimestampIncrementer())
				.start(bookReportStep)
				.build();
	}
}
