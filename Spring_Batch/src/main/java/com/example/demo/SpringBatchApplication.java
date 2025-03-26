package com.example.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@SpringBootApplication
@Configuration
public class SpringBatchApplication {

	@Bean
	public Step sampleStep(JobRepository jobRepository, DataSourceTransactionManager transactionManager) {
		return new StepBuilder("helloStep2", jobRepository).tasklet((contribution, chunkContext) -> {
			System.out.println("Hello world!");
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

	@Bean
	public Job sampleJob(JobRepository jobRepository, Step sampleStep) {
		return new JobBuilder("HelloJob", jobRepository).start(sampleStep).build();
	}

	public static void main(String[] args) {SpringApplication.run(SpringBatchApplication.class, args);}

}
