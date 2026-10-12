package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.batch.dto.PersonDTO;
import com.example.demo.batch.model.Person;
import com.example.demo.batch.processor.PersonItemProcessor;

/**
 * 第 1 課：Chunk —— 讀一筆、處理一筆，累積到 chunk 大小才一次寫入並 commit。
 *
 * <pre>
 * chunk = 3，CSV 有 4 行：
 *   交易 1：read Jill、Joe、Justin → process ×3 → write 3 筆 → commit
 *   交易 2：read Jane、(結束)      → process ×1 → write 1 筆 → commit
 * </pre>
 *
 * chunk 大小是取捨：越小 commit 越多次（每次 commit 都要和資料庫來回）；越大，失敗時 rollback 的範圍越大。
 * 實務上常從 100～1000 開始，依資料量與失敗的影響調整。
 *
 * <p>
 * 使用主程式的 Reader、Processor、Writer，只改變 chunk 大小。
 */
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class L01_ChunkTest extends PostgresContainerTestBase {

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private FlatFileItemReader<PersonDTO> reader;

	@Autowired
	private PersonItemProcessor processor;

	@Autowired
	private JdbcBatchItemWriter<Person> writer;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanUp() {
		deleteImportedDataAndHistory(jdbc);
	}

	private StepExecution runWithChunkSize(int chunkSize, ItemWriter<Person> writer) throws Exception {
		var step = new StepBuilder("chunk" + chunkSize, jobRepository)
				.<PersonDTO, Person>chunk(chunkSize)
				.transactionManager(transactionManager)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.build();
		JobExecution execution = jobOperator.start(
				new JobBuilder("chunkLesson" + chunkSize, jobRepository).start(step).build(), new JobParameters());
		return execution.getStepExecutions().iterator().next();
	}

	private int persons() {
		return jdbc.queryForObject("select count(*) from person", Integer.class);
	}

	// Spring Batch 6 的 chunk(size)（ChunkOrientedStep）：4 筆剛好整除時不會多 commit。
	// 舊的 chunk(size, transactionManager)（已標記為將移除）實測 chunk = 1、2、4 時分別 commit 5、3、2 次：
	// 最後多一個「讀到檔案結尾、沒有資料」的空 chunk，也 commit 一次
	@ParameterizedTest(name = "chunk = {0} → commit {1} 次")
	@CsvSource({ "1, 4", "2, 2", "3, 2", "4, 1", "10, 1" })
	void chunkSize_shouldDecideHowManyTimesToCommit(int chunkSize, int expectedCommits) throws Exception {
		var step = runWithChunkSize(chunkSize, writer);

		assertThat(step.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(step.getReadCount()).isEqualTo(4);
		assertThat(step.getWriteCount()).isEqualTo(4);
		assertThat(step.getCommitCount()).isEqualTo(expectedCommits);
		assertThat(persons()).isEqualTo(4);
	}

	// 寫入第 2 個 chunk 時失敗：只有這個 chunk rollback，第 1 個 chunk 已經 commit 的資料留在資料庫
	@Test
	void failure_shouldRollBackOnlyTheCurrentChunk() throws Exception {
		ItemWriter<Person> failsOnJane = chunk -> {
			if (chunk.getItems().stream().anyMatch(person -> person.firstName().equals("JANE"))) {
				throw new IllegalStateException("database unavailable");
			}
			writer.write(chunk);
		};

		var step = runWithChunkSize(3, failsOnJane);

		assertThat(step.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(step.getCommitCount()).isEqualTo(1);
		assertThat(step.getRollbackCount()).isEqualTo(1);
		assertThat(jdbc.queryForList("select first_name from person order by id", String.class))
				.containsExactly("JILL", "JOE", "JUSTIN");
	}
}
