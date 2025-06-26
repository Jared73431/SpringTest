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

import com.example.demo.batch.dto.PersonDTO;
import com.example.demo.batch.entity.Person;
import com.example.demo.batch.listener.JobCompleteionNotificationListener;
import com.example.demo.batch.processor.PersonItemProcessor;
import com.example.demo.batch.reader.PersonItemReader;
import com.example.demo.batch.writer.PersonItemWriter;

/**
 * Spring Batch作业的核心组件
 */
@Configuration
public class BatchConfiguration {

    @Autowired
    private PersonItemReader personItemReader;

    @Autowired
    private PersonItemProcessor personItemProcessor;

    @Autowired
    private PersonItemWriter personItemWriter;

    /**
     * 定义读取器 (Reader)
     */
    @Bean(name = "personReader")
    public FlatFileItemReader<PersonDTO> personItemReader() {
        return personItemReader.createReader();
    }

    /**
     * 定义作业 (Job)
     */
    @Bean(name = "importUserJob4")
    public Job importUserJob(JobRepository jobRepository,
                             @Qualifier("personStep") Step step1,
                             JobCompleteionNotificationListener listener,
                             BatchProperties properties) {
        return new JobBuilder("importUserJob4", jobRepository)
                .listener(listener)
                .start(step1)
                .build();
    }

    /**
     * 定义步骤 (Step)
     */
    @Bean(name = "personStep")
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      @Qualifier("personReader") FlatFileItemReader<PersonDTO> reader,
                      PersonItemProcessor processor,
                      PersonItemWriter writer) {
        return new StepBuilder("personStep", jobRepository)
                .<PersonDTO, Person> chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer.createWriter())
                .build();
    }
}
