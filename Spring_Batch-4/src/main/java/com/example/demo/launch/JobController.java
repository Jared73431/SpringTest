package com.example.demo.launch;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用名稱區分要執行哪一個 Job（第 3 課）。
 *
 * <pre>
 * GET  /api/jobs                       所有 Job 的名稱
 * POST /api/jobs/{jobName}/executions  在背景啟動，回應 202 與 Location
 * GET  /api/jobs/executions/{id}       查詢執行狀態
 * </pre>
 *
 * 修正前用 GET 觸發 Job（/api/batch/books/jpa…）：GET 不應該改變狀態，瀏覽器預先載入或重新整理都會再執行一次。
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private final JobLaunchService jobLaunchService;

	public JobController(JobLaunchService jobLaunchService) {
		this.jobLaunchService = jobLaunchService;
	}

	@GetMapping
	public Collection<String> jobNames() {
		return jobLaunchService.jobNames();
	}

	@PostMapping("/{jobName}/executions")
	public ResponseEntity<JobExecutionView> launch(@PathVariable String jobName,
			@RequestBody(required = false) Map<String, String> parameters) throws JobInstanceAlreadyCompleteException,
			JobExecutionAlreadyRunningException, JobRestartException, InvalidJobParametersException {
		var execution = jobLaunchService.launch(jobName, parameters == null ? Map.of() : parameters);
		return ResponseEntity.accepted()
				.location(URI.create("/api/jobs/executions/" + execution.getId()))
				.body(JobExecutionView.from(execution));
	}

	@GetMapping("/executions/{id}")
	public ResponseEntity<JobExecutionView> execution(@PathVariable long id) {
		return ResponseEntity.of(jobLaunchService.findExecution(id).map(JobExecutionView::from));
	}
}
