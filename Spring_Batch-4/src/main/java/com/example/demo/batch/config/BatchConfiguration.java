package com.example.demo.batch.config;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.batch.dto.PersonDTO;
import com.example.demo.batch.entity.Person;
import com.example.demo.batch.listener.JobCompleteionNotificationListener;
import com.example.demo.batch.processor.PersonItemProcessor;
import com.example.demo.batch.reader.PersonItemReader;
import com.example.demo.batch.repository.PersonRepository;
import com.example.demo.batch.writer.PersonItemWriter;

import jakarta.persistence.EntityManagerFactory;

/**
 * Spring Batch作业的核心组件
 */
@Configuration
public class BatchConfiguration {

    @Autowired
    private PersonItemReader personItemReader;

    /**
    事务管理器(處理資料庫連線資訊)
    已經通過Spring Boot的自動配置機制設置了數據庫連接,Spring Boot會自動根據這些屬性創建一個DataSource Bean，
    並將其註冊到應用上下文中。這是Spring Boot的自動配置功能的一部分，專門用於簡化這類常見配置。
    transactionManager方法會自動注入由Spring Boot創建的DataSource Bean，而不是使用您在配置類中定義的Bean。
     */
    //@Bean
    //public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    //    return new JpaTransactionManager (entityManagerFactory);
    //}

    /**
     * 读取器 (Reader)
     */
    @Bean
    public FlatFileItemReader<PersonDTO> reader() {
        return personItemReader.createReader();
    }
    //@Bean
    //public FlatFileItemReader<PersonDTO> reader(){
    //    return new FlatFileItemReaderBuilder<PersonDTO>()
    //            .name("personItemReader")
    //            .resource(new ClassPathResource("sample-data.csv"))//从sample-data.csv文件读取数据
    //            .delimited()
    //            .names(Arrays.stream(PersonDTO.class.getRecordComponents())
    //                    .map(RecordComponent::getName)
    //                    .toArray(String[]::new))//读取的数据被映射到Person对象，包含firstName和lastName字段
    //            .targetType(PersonDTO.class)
    //            .build();
    //}

    /**
     * 处理器 (Processor)
     * 定义了数据处理逻辑，使用PersonItemProcessor类处理读取的数据。
     */
    @Bean
    public PersonItemProcessor processor(){
        return new PersonItemProcessor();
    }

    /**
     * 写入器 (Writer)
     * 功能：将处理后的数据写入数据库
     * 实现：使用SQL将Person对象的数据插入person表
     */
    @Bean
    public RepositoryItemWriter<Person> writer(PersonRepository repository) {
        // 使用獨立的 PersonItemWriter 類
        return new PersonItemWriter(repository).createWriter();
    }
    //@Bean
    //public RepositoryItemWriter<Person> writer(PersonRepository repository) {
    //    return new RepositoryItemWriterBuilder<Person>()
    //            .repository(repository)
    //            .methodName("save")
    //            .build();
    //}

    /**
     * 作业定义 (Job)
     */
    @Bean(name = "importUserJob4")
    public Job importUserJob(JobRepository jobRepository, Step step1,
                             JobCompleteionNotificationListener listener, BatchProperties properties){
        return new JobBuilder("importUserJob4", jobRepository)//定义了一个名为"importUserJob4"的作业
                .listener(listener)//添加了一个监听器用于作业完成通知
                .start(step1)//设置了启动步骤
                .build();
    }

    /**
     * 步骤定义 (Step)
     */
    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      FlatFileItemReader<PersonDTO> reader,  // 修改为PersonDTO
                      PersonItemProcessor processor,
                      RepositoryItemWriter<Person> writer){  // 修改为RepositoryItemWriter<Person>
        return new StepBuilder("step14", jobRepository)
                .<PersonDTO, Person> chunk(3, transactionManager)  // 修改为PersonDTO和Person
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
