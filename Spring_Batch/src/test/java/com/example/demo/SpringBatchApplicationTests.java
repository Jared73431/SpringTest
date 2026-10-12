package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.batch.autoconfigure.JobExecutionExitCodeGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * 鎖定「最簡單的批次」的行為：啟動時執行 HelloJob、成功時 exit code 為 0、不是 web 應用程式。
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class SpringBatchApplicationTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobExecutionExitCodeGenerator exitCodeGenerator;

	// spring.batch.job.enabled 預設為 true：應用程式啟動時自動執行 HelloJob，執行紀錄存在 H2
	@Test
	void helloJob_shouldRunOnStartup(CapturedOutput output) {
		assertThat(output.getOut()).contains("Hello world!");

		var instances = jobRepository.getJobInstances("HelloJob", 0, 10);
		assertThat(instances).hasSize(1);
		var executions = jobRepository.getJobExecutions(instances.get(0));
		assertThat(executions).singleElement().satisfies(execution -> {
			assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
			assertThat(execution.getStepExecutions()).singleElement()
					.satisfies(step -> assertThat(step.getStepName()).isEqualTo("helloStep2"));
		});
	}

	// main 用 SpringApplication.exit 取得 exit code：Job COMPLETED 時為 0，
	// 讓 cron / Kubernetes CronJob 能判斷批次成功與否
	@Test
	void exitCode_shouldBeZero_whenJobCompleted() {
		assertThat(exitCodeGenerator.getExitCode()).isZero();
	}

	// classpath 上沒有 web：不啟動 web server，批次做完程式就結束，不會一直佔用 port
	@Test
	void application_shouldNotBeWebApplication() {
		assertThat(new SpringApplication(SpringBatchApplication.class).getWebApplicationType())
				.isEqualTo(WebApplicationType.NONE);
	}
}
