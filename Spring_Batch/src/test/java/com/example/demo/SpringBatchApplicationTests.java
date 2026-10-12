package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;

/**
 * Baseline：鎖定重構前的行為。
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class SpringBatchApplicationTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Environment environment;

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

	// application.properties 寫成 Server.port:8888（大寫 S）：Spring Boot 的 relaxed binding 比對屬性名稱時不分大小寫，
	// 所以其實有作用（分析時推測沒有作用，測試證明是錯的）
	@Test
	void serverPort_shouldBeBound_despiteTheCapitalLetter() {
		assertThat(environment.getProperty("server.port")).isEqualTo("8888");
	}
}
