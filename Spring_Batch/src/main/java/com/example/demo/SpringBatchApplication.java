package com.example.demo;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 最簡單的 Spring Batch：一個 Job，裡面一個 Tasklet Step，印出 Hello world!。
 *
 * <pre>
 * Job（HelloJob）
 *  └─ Step（helloStep2）
 *      └─ Tasklet：做一件事，回傳 FINISHED 表示做完了
 * </pre>
 *
 * 啟動時 Spring Boot 會自動執行所有 Job（spring.batch.job.enabled 預設 true），
 * 執行紀錄（JobInstance、JobExecution、StepExecution）存在 H2 記憶體資料庫，程式結束就消失。
 */
@SpringBootApplication
public class SpringBatchApplication {

	// 注入介面 PlatformTransactionManager，而不是具體類別 DataSourceTransactionManager：
	// 只要容器裡有任何一種 TransactionManager 就能啟動，不必知道它是哪一種實作
	@Bean
	public Step sampleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("helloStep2", jobRepository).tasklet((contribution, chunkContext) -> {
			System.out.println("Hello world!");
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

	@Bean
	public Job sampleJob(JobRepository jobRepository, Step sampleStep) {
		return new JobBuilder("HelloJob", jobRepository).start(sampleStep).build();
	}

	// 批次是「做完就結束」的程式：SpringApplication.exit 會依 Job 結果算出 exit code（COMPLETED 為 0，失敗不為 0），
	// 交給 System.exit，讓呼叫它的 cron、Kubernetes CronJob 或 CI 知道這次批次有沒有成功
	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(SpringBatchApplication.class, args)));
	}

}
