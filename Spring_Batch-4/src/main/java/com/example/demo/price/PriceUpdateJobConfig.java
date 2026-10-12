package com.example.demo.price;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * priceUpdateJob：套用價格更新檔，再匯出新的書籍報表。
 *
 * <pre>
 * priceUpdateStep ─ FAILED ─────────────────────────────→ Job FAILED
 *                 ─ COMPLETED WITH SKIPS → skipNoticeStep → bookReportStep
 *                 ─ 其他（COMPLETED）──────────────────→ bookReportStep
 * </pre>
 *
 * <ul>
 * <li>識別參數 input.file：同一個檔案套用完成後，不能再套用一次；失敗後用同一個檔案再執行，從中斷處繼續（第 5 課）</li>
 * <li>Skip：格式錯誤的行、不存在的 ISBN 跳過並記錄，最多 10 行（第 4 課）</li>
 * <li>Retry：資料庫暫時性錯誤（例如 deadlock）重試，最多 3 次（第 4 課）</li>
 * </ul>
 */
@Configuration
public class PriceUpdateJobConfig {

	private static final Logger log = LoggerFactory.getLogger(PriceUpdateJobConfig.class);

	public static final String INPUT_FILE = "input.file";

	/** 有跳過的行時，Step 的 exit status 改成這個值，讓 Job 走到 skipNoticeStep */
	public static final String COMPLETED_WITH_SKIPS = "COMPLETED WITH SKIPS";

	// @StepScope：要讀哪個檔案由這次執行的 JobParameters 決定
	@Bean
	@StepScope
	public FlatFileItemReader<PriceUpdate> priceUpdateReader(
			@Value("#{jobParameters['" + INPUT_FILE + "']}") Resource inputFile) {
		return new FlatFileItemReaderBuilder<PriceUpdate>()
				.name("priceUpdateReader")
				.resource(inputFile)
				.linesToSkip(1)
				.delimited()
				.names("isbn", "price")
				.targetType(PriceUpdate.class)
				.build();
	}

	@Bean
	public PriceUpdateProcessor priceUpdateProcessor(JdbcTemplate jdbcTemplate) {
		return new PriceUpdateProcessor(jdbcTemplate);
	}

	@Bean
	public JdbcBatchItemWriter<PriceUpdate> priceUpdateWriter(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<PriceUpdate>()
				.dataSource(dataSource)
				.sql("UPDATE book SET price = :price WHERE isbn = :isbn")
				.beanMapped()
				.build();
	}

	@Bean
	public Step priceUpdateStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FlatFileItemReader<PriceUpdate> priceUpdateReader, PriceUpdateProcessor priceUpdateProcessor,
			JdbcBatchItemWriter<PriceUpdate> priceUpdateWriter) {
		return new StepBuilder("priceUpdateStep", jobRepository)
				.<PriceUpdate, PriceUpdate>chunk(2)
				.transactionManager(transactionManager)
				.reader(priceUpdateReader)
				.processor(priceUpdateProcessor)
				.writer(priceUpdateWriter)
				.faultTolerant()
				.skip(FlatFileParseException.class, UnknownIsbnException.class)
				.skipLimit(10)
				.skipListener(new PriceUpdateSkipListener())
				.retry(TransientDataAccessException.class)
				.retryLimit(3)
				.listener(new SkipCountExitStatusListener())
				.build();
	}

	/** Step 完成但有跳過的行時，把 exit status 改成 COMPLETED WITH SKIPS（Job 依照 exit status 決定下一步） */
	static class SkipCountExitStatusListener implements StepExecutionListener {

		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			if (ExitStatus.COMPLETED.getExitCode().equals(stepExecution.getExitStatus().getExitCode())
					&& stepExecution.getSkipCount() > 0) {
				return new ExitStatus(COMPLETED_WITH_SKIPS);
			}
			return null;
		}
	}

	@Bean
	public Step skipNoticeStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("skipNoticeStep", jobRepository).tasklet((contribution, chunkContext) -> {
			// 實務上可以寄信或發通知；這裡只記錄
			long skipped = chunkContext.getStepContext().getStepExecution().getJobExecution().getStepExecutions()
					.stream()
					.filter(step -> step.getStepName().equals("priceUpdateStep"))
					.mapToLong(StepExecution::getSkipCount)
					.sum();
			log.warn("Price update skipped {} line(s), see the warnings of PriceUpdateSkipListener", skipped);
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

	@Bean
	public Job priceUpdateJob(JobRepository jobRepository, Step priceUpdateStep, Step skipNoticeStep,
			Step bookReportStep) {
		return new JobBuilder("priceUpdateJob", jobRepository)
				// 沒有 input.file 時直接拒絕（REST 回應 400），不會建立 JobInstance
				.validator(new DefaultJobParametersValidator(new String[] { INPUT_FILE }, new String[0]))
				.start(priceUpdateStep)
					.on("FAILED").fail()
				.from(priceUpdateStep).on(COMPLETED_WITH_SKIPS).to(skipNoticeStep).next(bookReportStep)
				.from(priceUpdateStep).on("*").to(bookReportStep)
				.end()
				.build();
	}
}
