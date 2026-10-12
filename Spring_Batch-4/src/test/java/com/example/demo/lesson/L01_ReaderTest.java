package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.DataClassRowMapper;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.book.Book;
import com.example.demo.lesson.jpa.BookEntity;
import com.example.demo.lesson.jpa.BookEntityRepository;
import com.example.demo.price.PriceUpdate;

import jakarta.persistence.EntityManagerFactory;

/**
 * 第 1 課：各種 Reader —— 同樣是 8 本書，用不同的方式讀。
 *
 * <pre>
 * Reader                  資料來源   怎麼讀
 * FlatFileItemReader      檔案       一次讀一行
 * JdbcCursorItemReader    資料庫     一個查詢，用 cursor 一筆一筆往下讀（連線一直開著）
 * JdbcPagingItemReader    資料庫     每次查一頁，下一頁用「sortKey > 上一頁最後一筆」查詢
 * JpaPagingItemReader     資料庫     每次查一頁（JPQL，用 OFFSET 分頁），回傳 Entity
 * RepositoryItemReader    資料庫     呼叫 Spring Data Repository 的分頁方法
 * </pre>
 *
 * 直接開啟 Reader 逐筆讀取（不經過 Job），比較讀到的內容。read() 回傳 null 表示讀完了。
 *
 * <p>
 * 選擇：沒有特別需求時，資料庫用 JdbcPagingItemReader（不必一直佔用連線、可以重新執行、第 6 課的情境也正確）；
 * 已經有 JPA Entity 和查詢時可以用 JPA 的 Reader，但 Entity 會經過 persistence context，資料量大時較慢。
 */
@SpringBootTest
class L01_ReaderTest extends PostgresContainerTestBase {

	private static final List<String> ALL_ISBNS_BY_ID = List.of("9781617297571", "9781491983461", "9780134685991",
			"9780321356680", "9780132350884", "9781617293566", "9781449373320", "9780201633610");

	@Autowired
	private DataSource dataSource;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private BookEntityRepository bookEntityRepository;

	@BeforeEach
	void cleanUp() {
		resetData(dataSource);
	}

	/** 開啟 Reader、讀到 null 為止、關閉：Step 執行時也是這樣使用 Reader */
	private static <T> List<String> readAll(ItemStreamReader<T> reader, Function<T, String> isbn) throws Exception {
		var isbns = new ArrayList<String>();
		reader.open(new ExecutionContext());
		try {
			for (T item = reader.read(); item != null; item = reader.read()) {
				isbns.add(isbn.apply(item));
			}
		}
		finally {
			reader.close();
		}
		return isbns;
	}

	@Test
	void flatFileItemReader_shouldReadOneLineAtATime_andFailOnTheBadLine() throws Exception {
		var reader = new FlatFileItemReaderBuilder<PriceUpdate>()
				.name("priceFile")
				.resource(new ClassPathResource("price-updates/price-update-2026-10-12.csv"))
				.linesToSkip(1)
				.delimited()
				.names("isbn", "price")
				.targetType(PriceUpdate.class)
				.build();
		reader.open(new ExecutionContext());

		assertThat(reader.read().isbn()).isEqualTo("9781617297571");
		assertThat(reader.read().isbn()).isEqualTo("9780134685991");
		// 第 4 行的價格是 abc：Reader 本身只會拋出例外，要不要跳過由 Step 的 skip 設定決定（第 4 課）
		assertThatThrownBy(reader::read).isInstanceOf(FlatFileParseException.class)
				.satisfies(e -> assertThat(((FlatFileParseException) e).getLineNumber()).isEqualTo(4));
		reader.close();
	}

	@Test
	void jdbcCursorItemReader_shouldReadEveryRowWithOneQuery() throws Exception {
		var reader = new JdbcCursorItemReaderBuilder<Book>()
				.name("cursor")
				.dataSource(dataSource)
				.sql("SELECT id, isbn, title, author, published_year, publisher, price FROM book ORDER BY id")
				.rowMapper(new DataClassRowMapper<>(Book.class))
				.build();

		assertThat(readAll(reader, Book::isbn)).isEqualTo(ALL_ISBNS_BY_ID);
	}

	@Test
	void jdbcPagingItemReader_shouldReadPageByPage() throws Exception {
		var reader = new JdbcPagingItemReaderBuilder<Book>()
				.name("paging")
				.dataSource(dataSource)
				.selectClause("SELECT id, isbn, title, author, published_year, publisher, price")
				.fromClause("FROM book")
				.sortKeys(Map.of("id", Order.ASCENDING))
				.pageSize(3) // 8 本書分 3 頁：3 + 3 + 2
				.rowMapper(new DataClassRowMapper<>(Book.class))
				.build();
		reader.afterPropertiesSet();

		assertThat(readAll(reader, Book::isbn)).isEqualTo(ALL_ISBNS_BY_ID);
	}

	@Test
	void jpaPagingItemReader_shouldReadEntitiesPageByPage() throws Exception {
		var reader = new JpaPagingItemReaderBuilder<BookEntity>()
				.name("jpa")
				.entityManagerFactory(entityManagerFactory)
				.queryString("SELECT b FROM BookEntity b ORDER BY b.id")
				.pageSize(3)
				.build();

		assertThat(readAll(reader, BookEntity::getIsbn)).isEqualTo(ALL_ISBNS_BY_ID);
	}

	// RepositoryItemReader 呼叫的方法，最後一個參數必須是 Pageable，回傳 Page（或 Slice）
	@Test
	void repositoryItemReader_shouldCallARepositoryMethodThatReturnsAPage() throws Exception {
		var reader = new RepositoryItemReaderBuilder<BookEntity>()
				.name("repository")
				.repository(bookEntityRepository)
				.methodName("findAll")
				.sorts(Map.of("id", Sort.Direction.ASC))
				.pageSize(3)
				.build();

		assertThat(readAll(reader, BookEntity::getIsbn)).isEqualTo(ALL_ISBNS_BY_ID);
	}

	// 修正前的 bookRepositoryReaderCustom 呼叫 findBooksAfterYear(Integer)，回傳 List、沒有 Pageable：
	// 執行時一定失敗，只是那個 Bean 沒有被任何 Step 使用，所以從未發現
	@Test
	void repositoryItemReader_shouldFail_whenTheMethodDoesNotTakeAPageable() {
		var reader = new RepositoryItemReaderBuilder<BookEntity>()
				.name("withoutPageable")
				.repository(bookEntityRepository)
				.methodName("findByPublishedYearGreaterThanEqual")
				.arguments(List.of(2020))
				.sorts(Map.of("id", Sort.Direction.ASC))
				.pageSize(3)
				.build();

		// 呼叫時在參數最後加上 PageRequest：找的是 findByPublishedYearGreaterThanEqual(Integer, PageRequest)
		assertThatThrownBy(() -> readAll(reader, BookEntity::getIsbn)).hasRootCauseInstanceOf(NoSuchMethodException.class);
	}
}
