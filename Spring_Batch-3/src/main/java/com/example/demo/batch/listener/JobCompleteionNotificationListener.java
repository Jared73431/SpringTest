package com.example.demo.batch.listener;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.batch.dto.Person;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class JobCompleteionNotificationListener implements JobExecutionListener {

    private final JdbcTemplate jdbcTemplate;

    public JobCompleteionNotificationListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void afterJob(JobExecution jobExecution){
        if(jobExecution.getStatus() == BatchStatus.COMPLETED){
            log.info("JOB FINISH! Time to verify the results");

            jdbcTemplate.query("SELECT first_name, last_name FROM person", new DataClassRowMapper<>(Person.class))
                    .forEach(person -> log.info("FOUND <{{}}> in the database", person));
        }
    }
}
