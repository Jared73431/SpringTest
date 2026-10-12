package com.example.demo.batch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.batch.dto.PersonDTO;
import com.example.demo.batch.listener.JobCompletionNotificationListener;
import com.example.demo.batch.model.Person;
import com.example.demo.batch.processor.PersonItemProcessor;

/**
 * CSV 匯入的 Job：讀 sample-data.csv → 姓名轉大寫 → 寫進 person 表。
 *
 * <pre>
 * importPersonJob
 *  └─ importPersonStep（chunk = 3）
 *      Reader：FlatFileItemReader   一次讀一行
 *      Processor：PersonItemProcessor  一次處理一筆
 *      Writer：JdbcBatchItemWriter   一次寫一整個 chunk（3 筆），寫完 commit
 * </pre>
 */
@Configuration
public class BatchConfiguration {

	/** 每讀 3 筆 commit 一次（第 1 課） */
	public static final int CHUNK_SIZE = 3;

	@Bean
	public FlatFileItemReader<PersonDTO> reader() {
		return new FlatFileItemReaderBuilder<PersonDTO>()
				.name("personItemReader")
				.resource(new ClassPathResource("sample-data.csv"))
				.delimited()
				.names("firstName", "lastName")
				.targetType(PersonDTO.class)
				.build();
	}

	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor();
	}

	// 修正前用 RepositoryItemWriter + JPA 的 save：一筆一個 insert。
	// JdbcBatchItemWriter 把一個 chunk 的 insert 用 JDBC batch 一次送出
	@Bean
	public JdbcBatchItemWriter<Person> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Person>()
				.dataSource(dataSource)
				.sql("INSERT INTO person (first_name, last_name) VALUES (:firstName, :lastName)")
				.beanMapped()
				.build();
	}

	// Spring Batch 6：chunk(size, transactionManager) 已標記為將移除，改用 chunk(size).transactionManager(...)
	@Bean
	public Step importPersonStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FlatFileItemReader<PersonDTO> reader, PersonItemProcessor processor, JdbcBatchItemWriter<Person> writer) {
		return new StepBuilder("importPersonStep", jobRepository)
				.<PersonDTO, Person>chunk(CHUNK_SIZE)
				.transactionManager(transactionManager)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.build();
	}

	// 沒有 JobParameters：每次都是同一個 JobInstance，所以只能成功執行一次（README「為什麼不能重新執行」）
	@Bean
	public Job importPersonJob(JobRepository jobRepository, Step importPersonStep,
			JobCompletionNotificationListener listener) {
		return new JobBuilder("importPersonJob", jobRepository)
				.listener(listener)
				.start(importPersonStep)
				.build();
	}
}
