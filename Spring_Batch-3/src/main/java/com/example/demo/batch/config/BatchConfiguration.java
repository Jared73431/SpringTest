package com.example.demo.batch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.example.demo.batch.dto.Person;
import com.example.demo.batch.listener.JobCompleteionNotificationListener;
import com.example.demo.batch.processor.PersonItemProcessor;

@Configuration
public class BatchConfiguration {

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public FlatFileItemReader<Person> reader(){
        return new FlatFileItemReaderBuilder<Person>()
                .name("personItemReader")
                .resource(new ClassPathResource("sample-data.csv"))
                .delimited()
                .names("firstName", "lastName")
                .targetType(Person.class)
                .build();
    }

    @Bean
    public PersonItemProcessor processor(){
        return new PersonItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Person>writer(DataSource dataSource){
        return new JdbcBatchItemWriterBuilder<Person>()
                .sql("INSERT INTO person(first_name, last_name) VALUES(:firstName, :lastName)")
                .dataSource(dataSource)
                .beanMapped()
                .build();
    }

    @Bean(name = "importUserJob4")
    public Job importUserJob(JobRepository jobRepository, Step step1,
                             JobCompleteionNotificationListener listener, BatchProperties properties){
        return new JobBuilder("importUserJob4", jobRepository)
                .listener(listener)
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      DataSourceTransactionManager transactionManager,
                      FlatFileItemReader<Person> reader,
                      PersonItemProcessor processor,
                      JdbcBatchItemWriter<Person> writer){
        return new StepBuilder("step14", jobRepository)
                .<Person, Person> chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
