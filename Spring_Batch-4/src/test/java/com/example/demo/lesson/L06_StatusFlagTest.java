package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.common.TimestampIncrementer;
import com.example.demo.lesson.jpa.BookOrderEntity;
import com.example.demo.order.BookOrder;

import jakarta.persistence.EntityManagerFactory;

/**
 * 第 6 課：狀態欄位 + 時間戳 —— 冪等的資料庫批次更新。
 *
 * <pre>
 * orderShippingJob
 *   Reader：SELECT ... WHERE status = 'PENDING'（JdbcPagingItemReader，pageSize = 3）
 *   Writer：UPDATE book_order SET status = 'SHIPPED' ... WHERE id = :id AND status = 'PENDING'
 *   參數：timestamp（每次都是新的 JobInstance）
 * </pre>
 *
 * 處理進度記錄在資料本身（status），不是記錄在 Spring Batch 的執行紀錄：
 * <ul>
 * <li>隨時都能再執行：已經出貨的訂單不會再被讀到</li>
 * <li>中途失敗不必「重新執行同一個 JobInstance」：下一次執行（新的 timestamp）自然只處理剩下的訂單</li>
 * </ul>
 *
 * 和第 5 課的差別：價格更新檔的進度只能靠 Reader 記住「讀到第幾行」，所以必須用同一個 JobInstance 重新執行；
 * 訂單的進度在資料庫裡，所以每次都可以是新的 JobInstance。
 */
@SpringBootTest
class L06_StatusFlagTest extends PostgresContainerTestBase {

	@MockitoSpyBean
	private JdbcBatchItemWriter<BookOrder> shipOrderWriter;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job orderShippingJob;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	private final AtomicBoolean databaseDown = new AtomicBoolean();

	@BeforeEach
	void cleanUp() throws Exception {
		resetData(dataSource);
		// 資料庫斷線時，寫入訂單 4 所在的 chunk（第 2 個 chunk）會失敗
		doAnswer(invocation -> {
			Chunk<BookOrder> chunk = invocation.getArgument(0);
			if (databaseDown.get() && chunk.getItems().stream().anyMatch(order -> order.id() == 4)) {
				throw new DataAccessResourceFailureException("database unavailable");
			}
			return invocation.callRealMethod();
		}).when(shipOrderWriter).write(any());
	}

	private int orders(String status) {
		return jdbc.queryForObject("select count(*) from book_order where status = ?", Integer.class, status);
	}

	private static long readCount(JobExecution execution) {
		return execution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
	}

	@Test
	void everyRun_shouldBeANewJobInstance_andOnlyReadPendingOrders() {
		var first = jobOperator.startNextInstance(orderShippingJob);
		var second = jobOperator.startNextInstance(orderShippingJob);

		assertThat(readCount(first)).isEqualTo(10);
		assertThat(readCount(second)).isZero(); // 已經出貨的訂單不會再被讀到
		assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(second.getJobInstance().getId()).isNotEqualTo(first.getJobInstance().getId());
		assertThat(second.getJobParameters().getLong(TimestampIncrementer.KEY)).isNotNull();
		assertThat(orders("SHIPPED")).isEqualTo(10);
	}

	@Test
	void nextRun_shouldFinishTheRemainingOrders_afterAFailure() {
		databaseDown.set(true);
		var failed = jobOperator.startNextInstance(orderShippingJob);

		assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(orders("SHIPPED")).isEqualTo(3); // 第 1 個 chunk 已經 commit

		databaseDown.set(false);
		var next = jobOperator.startNextInstance(orderShippingJob); // 新的 JobInstance，不是重新執行

		assertThat(next.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(readCount(next)).isEqualTo(7);
		assertThat(orders("PENDING")).isZero();
	}

	// 用 OFFSET 分頁的 Reader（JpaPagingItemReader）讀「WHERE status = 'PENDING'」，同時又把 status 改掉：
	// 第 2 頁的 OFFSET 3 是在「剩下的待出貨訂單」中跳過 3 筆，於是有訂單被跳過
	@Test
	void offsetPagingReader_shouldMissOrders_whenTheFilterColumnIsUpdated() throws Exception {
		var offsetReader = new JpaPagingItemReaderBuilder<BookOrderEntity>()
				.name("offsetPagingReader")
				.entityManagerFactory(entityManagerFactory)
				.queryString("SELECT o FROM BookOrderEntity o WHERE o.status = 'PENDING' ORDER BY o.id")
				.pageSize(3)
				.build();
		var step = new StepBuilder("offsetPagingStep", jobRepository)
				.<BookOrderEntity, BookOrder>chunk(3)
				.transactionManager(transactionManager)
				.reader(offsetReader)
				.processor(o -> new BookOrder(o.getId(), o.getBookId(), o.getQuantity(), o.getStatus()))
				.writer(shipOrderWriter)
				.build();
		var job = new JobBuilder("offsetPagingJob", jobRepository).incrementer(new TimestampIncrementer())
				.start(step).build();

		var execution = jobOperator.startNextInstance(job);

		// 第 1 頁 OFFSET 0：訂單 1、2、3 → 出貨
		// 第 2 頁 OFFSET 3：剩下的 4～10 跳過 3 筆 → 讀到 7、8、9 → 出貨
		// 第 3 頁 OFFSET 6：剩下的 4、5、6、10 只有 4 筆 → 讀不到 → 結束
		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED); // Job 顯示成功，卻漏了 4 筆
		assertThat(readCount(execution)).isEqualTo(6);
		assertThat(jdbc.queryForList("select id from book_order where status = 'PENDING' order by id", Long.class))
				.containsExactly(4L, 5L, 6L, 10L);
	}
}
