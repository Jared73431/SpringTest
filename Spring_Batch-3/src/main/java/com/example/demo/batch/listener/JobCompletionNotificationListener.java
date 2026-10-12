package com.example.demo.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Job 結束後，印出每個 Step 讀了幾筆、寫了幾筆、commit 幾次，以及 person 表目前的筆數。
 *
 * <p>
 * 修正前用 findAll() 把整張表讀出來逐筆印出：資料越多越慢。筆數用 count(*) 查詢即可；每一筆的轉換已經由 Processor 記錄。
 */
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

	private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

	private final JdbcTemplate jdbcTemplate;

	public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		log.info("Job {} finished with status {}", jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
		jobExecution.getStepExecutions().forEach(step -> log.info("Step {}: read={}, written={}, commits={}",
				step.getStepName(), step.getReadCount(), step.getWriteCount(), step.getCommitCount()));
		log.info("Total records in person table: {}",
				jdbcTemplate.queryForObject("select count(*) from person", Long.class));
	}
}
