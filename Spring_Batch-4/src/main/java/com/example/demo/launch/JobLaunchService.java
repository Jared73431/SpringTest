package com.example.demo.launch;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

/**
 * 依照名稱找到 Job，在背景執行，立刻回傳 JobExecution（狀態 STARTING）。
 *
 * <p>
 * 修正前在 Controller 中同步執行：HTTP 請求要等 Job 做完才回應，Job 一久就逾時。Batch-4 原本的 @Async 少了
 * {@code @EnableAsync}，所以其實也是同步執行。
 *
 * <p>
 * 非同步的 JobOperator 只在這裡使用，不宣告成 Bean：宣告成 Bean 會取代 Spring Boot 的 JobOperator，連其他地方
 * （例如測試）啟動的 Job 都變成非同步。
 */
@Service
public class JobLaunchService {

	private final JobRegistry jobRegistry;

	private final JobRepository jobRepository;

	private final TaskExecutorJobOperator asyncJobOperator;

	public JobLaunchService(JobRegistry jobRegistry, JobRepository jobRepository,
			@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) throws Exception {
		this.jobRegistry = jobRegistry;
		this.jobRepository = jobRepository;
		this.asyncJobOperator = new TaskExecutorJobOperator();
		this.asyncJobOperator.setJobRegistry(jobRegistry);
		this.asyncJobOperator.setJobRepository(jobRepository);
		this.asyncJobOperator.setTaskExecutor(taskExecutor);
		this.asyncJobOperator.afterPropertiesSet();
	}

	public Collection<String> jobNames() {
		return new TreeSet<>(jobRegistry.getJobNames());
	}

	/**
	 * 沒有參數、而且 Job 有 incrementer（timestamp）時，自動產生下一組參數；否則使用傳入的參數（String、參與識別）。
	 */
	public JobExecution launch(String jobName, Map<String, String> parameters) throws JobInstanceAlreadyCompleteException,
			JobExecutionAlreadyRunningException, JobRestartException, InvalidJobParametersException {
		Job job = jobRegistry.getJob(jobName);
		if (job == null) {
			throw new JobNotFoundException(jobName);
		}
		if (parameters.isEmpty() && job.getJobParametersIncrementer() != null) {
			return asyncJobOperator.startNextInstance(job);
		}
		var builder = new JobParametersBuilder();
		parameters.forEach(builder::addString);
		return asyncJobOperator.start(job, builder.toJobParameters());
	}

	// 實測：Spring Batch 6 的 JDBC JobRepository 找不到時不是回傳 null，而是拋出 EmptyResultDataAccessException
	public Optional<JobExecution> findExecution(long executionId) {
		try {
			return Optional.ofNullable(jobRepository.getJobExecution(executionId));
		}
		catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	public static class JobNotFoundException extends RuntimeException {

		public JobNotFoundException(String jobName) {
			super("No job named " + jobName);
		}
	}
}
