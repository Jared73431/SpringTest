package com.example.demo.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
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
import com.example.demo.batch.repository.HealthInsuranceRepository;
import com.example.demo.batch.writer.HealthInsuranceItemWriter;

@Configuration
public class HealthInsuranceBatchConfig {

    @Autowired
    private HealthInsuranceItemReader healthInsuranceItemReader;

    @Autowired
    private HealthInsuranceItemProcessor healthInsuranceItemProcessor;

    @Autowired
    private HealthInsuranceItemWriter healthInsuranceItemWriter; // 直接注入组件

    @Bean(name = "healthInsuranceReader")
    public FlatFileItemReader<HealthInsuranceDTO> healthInsuranceItemReader() {
        return healthInsuranceItemReader.createReader();
    }

    @Bean(name = "healthInsuranceJob")
    public Job importUserJob(JobRepository jobRepository, @Qualifier("healthInsuranceStep") Step step1,
                             JobCompleteionNotificationListener listener, BatchProperties properties){
        return new JobBuilder("healthInsuranceJob", jobRepository)//定义了一个名为"importUserJob4"的作业
                .listener(listener)//添加了一个监听器用于作业完成通知
                .start(step1)//设置了启动步骤
                .build();
    }

    @Bean(name = "healthInsuranceStep")
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      @Qualifier("healthInsuranceReader") FlatFileItemReader<HealthInsuranceDTO> reader,  // 修改为PersonDTO
                      HealthInsuranceItemProcessor processor,
                      HealthInsuranceItemWriter writer){  // 修改为RepositoryItemWriter<Person>
        return new StepBuilder("HealthInsuranceStep", jobRepository)
                .<HealthInsuranceDTO, HealthInsurance> chunk(3, transactionManager)  // 修改为PersonDTO和Person
                .reader(reader)
                .processor(processor)
                .writer(writer.createWriter())
                .build();
    }
}
