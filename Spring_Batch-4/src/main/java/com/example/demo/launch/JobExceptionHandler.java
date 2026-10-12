package com.example.demo.launch;

import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.launch.JobLaunchService.JobNotFoundException;

/**
 * 啟動 Job 失敗的原因轉成 ProblemDetail。
 *
 * <pre>
 * 404  沒有這個 Job
 * 400  JobParameters 不正確（例如 priceUpdateJob 少了 input.file）
 * 409  這組參數已經完成、正在執行、或不能重新執行
 * </pre>
 */
@RestControllerAdvice
public class JobExceptionHandler {

	@ExceptionHandler(JobNotFoundException.class)
	public ProblemDetail jobNotFound(JobNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(InvalidJobParametersException.class)
	public ProblemDetail invalidParameters(InvalidJobParametersException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler({ JobInstanceAlreadyCompleteException.class, JobExecutionAlreadyRunningException.class,
			JobRestartException.class })
	public ProblemDetail conflict(Exception e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}
}
