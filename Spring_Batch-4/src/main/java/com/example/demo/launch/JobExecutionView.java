package com.example.demo.launch;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

/**
 * 回應用的 JobExecution：只取需要的欄位，不直接回傳 Spring Batch 的物件（循環參照、內部欄位）。
 */
public record JobExecutionView(long id, String jobName, Map<String, String> parameters, String status,
		String exitCode, LocalDateTime startTime, LocalDateTime endTime, List<StepView> steps) {

	public record StepView(String name, String status, String exitCode, long readCount, long writeCount,
			long skipCount, long commitCount, long rollbackCount) {

		static StepView from(StepExecution step) {
			return new StepView(step.getStepName(), step.getStatus().name(), step.getExitStatus().getExitCode(),
					step.getReadCount(), step.getWriteCount(), step.getSkipCount(), step.getCommitCount(),
					step.getRollbackCount());
		}
	}

	public static JobExecutionView from(JobExecution execution) {
		Map<String, String> parameters = execution.getJobParameters().parameters().stream()
				.collect(Collectors.toMap(p -> p.name(), p -> String.valueOf(p.value())));
		List<StepView> steps = execution.getStepExecutions().stream()
				.sorted(Comparator.comparing(StepExecution::getId))
				.map(StepView::from)
				.toList();
		return new JobExecutionView(execution.getId(), execution.getJobInstance().getJobName(), parameters,
				execution.getStatus().name(), execution.getExitStatus().getExitCode(), execution.getStartTime(),
				execution.getEndTime(), steps);
	}
}
