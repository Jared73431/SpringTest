package com.example.demo.batch.reader;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;

import com.example.demo.batch.entity.Book;
import com.example.demo.batch.repository.BookRepository;

import jakarta.persistence.EntityManagerFactory;

/**
 * 提供三種不同的資料庫讀取器實現
 * 1. RepositoryItemReader - 使用 Spring Data Repository
 * 2. JpaPagingItemReader - 使用 JPA 分頁查詢
 * 3. JdbcCursorItemReader - 使用 JDBC 游標讀取
 */
@Component
public class BookItemReaders {

    private final BookRepository bookRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    public BookItemReaders(BookRepository bookRepository,
                           EntityManagerFactory entityManagerFactory,
                           DataSource dataSource) {
        this.bookRepository = bookRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
    }

    /**
     * 方法1: 使用 RepositoryItemReader
     * 適用於: 簡單的 Repository 方法調用，自動分頁
     * 優點: 配置簡單，與 Spring Data 整合良好
     * 缺點: 靈活性較低，主要依賴 Repository 方法
     */
    public RepositoryItemReader<Book> createRepositoryItemReader() {
        return new RepositoryItemReaderBuilder<Book>()
                .name("bookRepositoryItemReader")
                .repository(bookRepository)
                .methodName("findAll") // 使用 Repository 的方法名
                .pageSize(10) // 每頁讀取數量
                .sorts(Map.of("id", Sort.Direction.ASC)) // 排序規則
                .build();
    }

    /**
     * 方法2: 使用 RepositoryItemReader 配合自定義查詢方法
     * 查找2020年後出版的書籍
     */
    public RepositoryItemReader<Book> createRepositoryItemReaderWithCustomQuery() {
        return new RepositoryItemReaderBuilder<Book>()
                .name("bookRepositoryItemReaderCustom")
                .repository(bookRepository)
                .methodName("findBooksAfterYear") // 自定義查詢方法
                .arguments(2020) // 方法參數
                .pageSize(5)
                .sorts(Map.of("year", Sort.Direction.ASC, "id", Sort.Direction.ASC))
                .build();
    }

    /**
     * 方法3: 使用 JpaPagingItemReader
     * 適用於: 複雜的 JPQL 查詢，需要更多控制
     * 優點: 支援複雜查詢，參數化查詢，性能較好
     * 缺點: 配置相對複雜
     */
    public JpaPagingItemReader<Book> createJpaPagingItemReader() {
        return new JpaPagingItemReaderBuilder<Book>()
                .name("bookJpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT b FROM Book b WHERE b.cost > :minCost ORDER BY b.cost DESC")
                .parameterValues(Map.of("minCost", 50.0)) // 查詢參數
                .pageSize(8)
                .build();
    }

    /**
     * 方法4: 使用 JpaPagingItemReader 進行複雜查詢
     * 查找特定價格範圍和年份的書籍
     */
    public JpaPagingItemReader<Book> createJpaPagingItemReaderComplex() {
        return new JpaPagingItemReaderBuilder<Book>()
                .name("bookJpaPagingItemReaderComplex")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                    SELECT b FROM Book b 
                    WHERE  b.cost BETWEEN :minCost AND :maxCost 
                    AND    b.year >= :minYear 
                    ORDER  BY b.year DESC, b.cost ASC
                    """)
                .parameterValues(Map.of(
                        "minCost", 20.0,
                        "maxCost", 100.0,
                        "minYear", 2010
                ))
                .pageSize(12)
                .build();
    }

    /**
     * 方法5: 使用 JdbcCursorItemReader
     * 適用於: 大數據量處理，原生 SQL 查詢
     * 優點: 性能最佳，記憶體占用低，支援原生 SQL
     * 缺點: 需要手動映射，配置較複雜
     */
    public JdbcCursorItemReader<Book> createJdbcCursorItemReader() {
        return new JdbcCursorItemReaderBuilder<Book>()
                .name("bookJdbcCursorItemReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT id, isbn, title, author, year, publisher, cost 
                    FROM book 
                    WHERE cost > ? 
                    ORDER BY year DESC, cost ASC
                    """)
                .preparedStatementSetter(ps -> ps.setDouble(1, 30.0)) // SQL 參數設置
                .rowMapper(new BeanPropertyRowMapper<>(Book.class)) // 行映射器
                .fetchSize(100) // 每次從資料庫獲取的記錄數
                .build();
    }

    /**
     * 方法6: 使用 JdbcCursorItemReader 進行複雜 SQL 查詢
     * 包含統計和聚合操作
     */
    public JdbcCursorItemReader<Book> createJdbcCursorItemReaderComplex() {
        return new JdbcCursorItemReaderBuilder<Book>()
                .name("bookJdbcCursorItemReaderComplex")
                .dataSource(dataSource)
                .sql("""
                    SELECT b.id, b.isbn, b.title, b.author, b.year, b.publisher, b.cost 
                    FROM book b 
                    WHERE b.author IN (
                        SELECT DISTINCT author 
                        FROM book 
                        GROUP BY author 
                        HAVING COUNT(*) > 1
                    ) 
                    AND b.year BETWEEN ? AND ?
                    ORDER BY b.author, b.year DESC
                    """)
                .preparedStatementSetter(ps -> {
                    ps.setInt(1, 2000); // 起始年份
                    ps.setInt(2, 2023); // 結束年份
                })
                .rowMapper(new BeanPropertyRowMapper<>(Book.class))
                .fetchSize(50)
                .build();
    }
}
