package com.example.demo.batch.listener;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import com.example.demo.batch.repository.PersonRepository;

import lombok.extern.log4j.Log4j2;

/**
 * 关键功能:
 *
 * 使用@Component注解，表明这是一个Spring管理的组件
 * 依赖注入JdbcTemplate用于数据库操作
 * 实现afterJob方法，在作业完成后被调用
 * 当作业状态为COMPLETED时，查询数据库中的person表
 * 使用DataClassRowMapper将查询结果映射到Person对象
 * 遍历并记录所有查询到的人员信息
 *
 * 这个监听器的主要目的是验证批处理作业是否成功完成，通过查询数据库确认数据已被正确写入。
 */
@Log4j2
@Component
public class JobCompleteionNotificationListener implements JobExecutionListener {

    private final PersonRepository personRepository;;

    // 构造函数
    public JobCompleteionNotificationListener(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    // 作业完成后的回调方法
    @Override
    public void afterJob(JobExecution jobExecution){
        if(jobExecution.getStatus() == BatchStatus.COMPLETED){
            log.info("JOB FINISH! Time to verify the results");

            long count = personRepository.count();
            log.info("Total records in database: {}", count);

            if (count > 0) {
                personRepository.findAll()
                        .forEach(person -> log.info("FOUND <{}> in the database", person));
            } else {
                log.warn("No records found in the database!");
            }
        }
    }
}
