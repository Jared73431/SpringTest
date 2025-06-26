package com.example.demo.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.batch.dto.BookDTO;
import com.example.demo.batch.entity.Book;
import com.example.demo.batch.listener.BookJobCompletionListener;
import com.example.demo.batch.processor.BookItemProcessor;
import com.example.demo.batch.reader.BookItemReaders;
import com.example.demo.batch.writer.BookItemWriter;

/**
 * 書籍批次處理配置類
 * 定義三種不同的讀取器方式和對應的 Job 配置
 */
@Configuration
public class BookBatchConfiguration {

    @Autowired
    private BookItemReaders bookItemReaders;

    @Autowired
    private BookItemProcessor bookItemProcessor;

    @Autowired
    private BookItemWriter bookItemWriter;

    // ================================
    // 讀取器 Bean 定義
    // ================================

    /**
     * RepositoryItemReader - 使用 Spring Data Repository
     */
    @Bean(name = "bookRepositoryReader")
    public RepositoryItemReader<Book> bookRepositoryReader() {
        return bookItemReaders.createRepositoryItemReader();
    }

    /**
     * RepositoryItemReader - 使用自定義查詢
     */
    @Bean(name = "bookRepositoryReaderCustom")
    public RepositoryItemReader<Book> bookRepositoryReaderCustom() {
        return bookItemReaders.createRepositoryItemReaderWithCustomQuery();
    }

    /**
     * JpaPagingItemReader - 使用 JPA 分頁
     */
    @Bean(name = "bookJpaReader")
    public JpaPagingItemReader<Book> bookJpaReader() {
        return bookItemReaders.createJpaPagingItemReader();
    }

    /**
     * JpaPagingItemReader - 複雜查詢
     */
    @Bean(name = "bookJpaReaderComplex")
    public JpaPagingItemReader<Book> bookJpaReaderComplex() {
        return bookItemReaders.createJpaPagingItemReaderComplex();
    }

    /**
     * JdbcCursorItemReader - 使用 JDBC 游標
     */
    @Bean(name = "bookJdbcReader")
    public JdbcCursorItemReader<Book> bookJdbcReader() {
        return bookItemReaders.createJdbcCursorItemReader();
    }

    /**
     * JdbcCursorItemReader - 複雜 SQL
     */
    @Bean(name = "bookJdbcReaderComplex")
    public JdbcCursorItemReader<Book> bookJdbcReaderComplex() {
        return bookItemReaders.createJdbcCursorItemReaderComplex();
    }

    // ================================
    // 寫入器 Bean 定義
    // ================================

    @Bean(name = "bookConsoleWriter")
    public ItemWriter<BookDTO> bookConsoleWriter() {
        return bookItemWriter;
    }

    @Bean(name = "bookCsvWriter")
    public ItemWriter<BookDTO> bookCsvWriter() {
        return bookItemWriter.createCsvWriter();
    }

    @Bean(name = "bookReportWriter")
    public ItemWriter<BookDTO> bookReportWriter() {
        return bookItemWriter.createCategoryReportWriter();
    }

    // ================================
    // 步驟 (Step) 定義
    // ================================

    /**
     * 使用 RepositoryItemReader 的步驟
     */
    @Bean(name = "bookRepositoryStep")
    public Step bookRepositoryStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   @Qualifier("bookRepositoryReader") RepositoryItemReader<Book> reader,
                                   @Qualifier("bookConsoleWriter") ItemWriter<BookDTO> writer) {
        return new StepBuilder("bookRepositoryStep", jobRepository)
                .<Book, BookDTO>chunk(5, transactionManager)
                .reader(reader)
                .processor(bookItemProcessor)
                .writer(writer)
                .build();
    }

    /**
     * 使用 JpaPagingItemReader 的步驟
     */
    @Bean(name = "bookJpaStep")
    public Step bookJpaStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            @Qualifier("bookJpaReader") JpaPagingItemReader<Book> reader,
                            @Qualifier("bookCsvWriter") ItemWriter<BookDTO> writer) {
        return new StepBuilder("bookJpaStep", jobRepository)
                .<Book, BookDTO>chunk(8, transactionManager)
                .reader(reader)
                .processor(bookItemProcessor)
                .writer(writer)
                .build();
    }

    /**
     * 使用 JdbcCursorItemReader 的步驟
     */
    @Bean(name = "bookJdbcStep")
    public Step bookJdbcStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             @Qualifier("bookJdbcReader") JdbcCursorItemReader<Book> reader,
                             @Qualifier("bookReportWriter") ItemWriter<BookDTO> writer) {
        return new StepBuilder("bookJdbcStep", jobRepository)
                .<Book, BookDTO>chunk(10, transactionManager)
                .reader(reader)
                .processor(bookItemProcessor)
                .writer(writer)
                .build();
    }

    /**
     * 複合步驟 - 使用多個讀取器的複雜步驟
     */
    @Bean(name = "bookComplexStep")
    public Step bookComplexStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                @Qualifier("bookJpaReaderComplex") JpaPagingItemReader<Book> reader,
                                @Qualifier("bookConsoleWriter") ItemWriter<BookDTO> writer) {
        return new StepBuilder("bookComplexStep", jobRepository)
                .<Book, BookDTO>chunk(12, transactionManager)
                .reader(reader)
                .processor(bookItemProcessor)
                .writer(writer)
                .allowStartIfComplete(true) // 允許重複執行
                .build();
    }

    // ================================
    // 作業 (Job) 定義
    // ================================

    /**
     * 使用 RepositoryItemReader 的作業
     */
    @Bean(name = "bookRepositoryJob")
    public Job bookRepositoryJob(JobRepository jobRepository,
                                 @Qualifier("bookRepositoryStep") Step step,
                                 BookJobCompletionListener listener) {
        return new JobBuilder("bookRepositoryJob", jobRepository)
                .listener(listener)
                .start(step)
                .build();
    }

    /**
     * 使用 JpaPagingItemReader 的作業
     */
    @Bean(name = "bookJpaJob")
    public Job bookJpaJob(JobRepository jobRepository,
                          @Qualifier("bookJpaStep") Step step,
                          BookJobCompletionListener listener) {
        return new JobBuilder("bookJpaJob", jobRepository)
                .listener(listener)
                .start(step)
                .build();
    }

    /**
     * 使用 JdbcCursorItemReader 的作業
     */
    @Bean(name = "bookJdbcJob")
    public Job bookJdbcJob(JobRepository jobRepository,
                           @Qualifier("bookJdbcStep") Step step,
                           BookJobCompletionListener listener) {
        return new JobBuilder("bookJdbcJob", jobRepository)
                .listener(listener)
                .start(step)
                .build();
    }

    /**
     * 綜合作業 - 包含多個步驟的完整流程
     */
    @Bean(name = "bookComprehensiveJob")
    public Job bookComprehensiveJob(JobRepository jobRepository,
                                    @Qualifier("bookRepositoryStep") Step repositoryStep,
                                    @Qualifier("bookJpaStep") Step jpaStep,
                                    @Qualifier("bookJdbcStep") Step jdbcStep,
                                    BookJobCompletionListener listener) {
        return new JobBuilder("bookComprehensiveJob", jobRepository)
                .listener(listener)
                .start(repositoryStep) // 第一步：Repository 讀取
                .next(jpaStep)        // 第二步：JPA 分頁讀取
                .next(jdbcStep)       // 第三步：JDBC 游標讀取
                .build();
    }

    /**
     * 條件作業 - 根據條件執行不同步驟
     */
    @Bean(name = "bookConditionalJob")
    public Job bookConditionalJob(JobRepository jobRepository,
                                  @Qualifier("bookComplexStep") Step complexStep,
                                  @Qualifier("bookRepositoryStep") Step simpleStep,
                                  BookJobCompletionListener listener) {
        return new JobBuilder("bookConditionalJob", jobRepository)
                .listener(listener)
                .start(complexStep)
                .on("COMPLETED").to(simpleStep) // 如果複雜步驟完成，執行簡單步驟
                .on("FAILED").end() // 如果失敗，結束作業
                .end()
                .build();
    }
}
