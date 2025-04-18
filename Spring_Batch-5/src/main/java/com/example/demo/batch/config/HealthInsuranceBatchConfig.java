package com.example.demo.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.batch.dto.HealthInsuranceDTO;
import com.example.demo.batch.entity.HealthInsurance;
import com.example.demo.batch.listener.JobCompleteionNotificationListener;
import com.example.demo.batch.processor.HealthInsuranceItemProcessor;
import com.example.demo.batch.reader.HealthInsuranceItemReader;
import com.example.demo.batch.writer.HealthInsuranceItemWriter;

@Configuration
public class HealthInsuranceBatchConfig {

    @Autowired
    private HealthInsuranceItemReader healthInsuranceItemReader;

    @Autowired
    private HealthInsuranceItemProcessor healthInsuranceItemProcessor;

    @Autowired
    private HealthInsuranceItemWriter healthInsuranceItemWriter;

    /**
     * 定义读取器 (Reader)
     */
    @Bean(name = "healthInsuranceReader")
    public FlatFileItemReader<HealthInsuranceDTO> healthInsuranceItemReader() {
        return healthInsuranceItemReader.createReader();
    }

    /**
     * 定义作业 (Job)
     */
    @Bean(name = "healthInsuranceJob")
    public Job healthInsuranceJob(JobRepository jobRepository,
                                  @Qualifier("healthInsuranceStep") Step step1,
                                  JobCompleteionNotificationListener listener,
                                  BatchProperties properties) {
        return new JobBuilder("healthInsuranceJob", jobRepository)
                .listener(listener)
                .start(step1)
                .build();
    }

    /**
     * 定义步骤 (Step)
     */
    @Bean(name = "healthInsuranceStep")
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      @Qualifier("healthInsuranceReader") FlatFileItemReader<HealthInsuranceDTO> reader,
                      HealthInsuranceItemProcessor processor,
                      HealthInsuranceItemWriter writer) {
        return new StepBuilder("healthInsuranceStep", jobRepository)
                .<HealthInsuranceDTO, HealthInsurance> chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer.createWriter())
                .build();
    }
}
