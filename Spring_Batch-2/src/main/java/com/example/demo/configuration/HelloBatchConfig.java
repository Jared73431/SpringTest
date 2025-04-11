package com.example.demo.configuration;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class HelloBatchConfig {

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public Step sampleStep(JobRepository jobRepository, DataSourceTransactionManager transactionManager) {
        return new StepBuilder("helloStep8", jobRepository).tasklet((contribution, chunkContext) -> {
            System.out.println("Hello world2!");
            return RepeatStatus.FINISHED;
        }, transactionManager).build();
    }

    @Bean
    public Job sampleJob(JobRepository jobRepository, Step sampleStep) {
        return new JobBuilder("HelloJob8", jobRepository).start(sampleStep).build();
    }

}
