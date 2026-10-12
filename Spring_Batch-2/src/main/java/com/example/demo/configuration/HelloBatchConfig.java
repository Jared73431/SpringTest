package com.example.demo.configuration;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 和 Spring_Batch 一樣是一個 Tasklet，差別在 JobRepository 存在 PostgreSQL：程式結束後，執行紀錄仍然留在 BATCH_* 表。
 *
 * <p>
 * Bean 名稱和 Job / Step 名稱一致（helloJob、helloStep），在 BATCH_* 表看到的名稱就是程式中的名稱。
 */
@Configuration
public class HelloBatchConfig {

	// 修正前自己宣告了 DataSourceTransactionManager Bean：Spring Boot 本來就會建立，不需要
	@Bean
	public Step helloStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("helloStep", jobRepository).tasklet((contribution, chunkContext) -> {
			System.out.println("Hello world2! jobParameters=" + chunkContext.getStepContext().getJobParameters());
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

	@Bean
	public Job helloJob(JobRepository jobRepository, Step helloStep) {
		return new JobBuilder("helloJob", jobRepository).start(helloStep).build();
	}

}
