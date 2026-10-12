package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Baseline：鎖定合併前 Spring_Quartz 的行為。
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class SpringQuartzApplicationTests {

	@Autowired
	private Scheduler scheduler;

	// 自己建立的 SchedulerFactoryBean + MethodInvokingJobDetailFactoryBean：每秒呼叫 QusrtzTask.sayHello()
	@Test
	void sayHello_shouldRunEverySecond(CapturedOutput output) throws Exception {
		var triggerKeys = scheduler.getTriggerKeys(GroupMatcher.anyGroup());
		assertThat(triggerKeys).singleElement()
				.satisfies(key -> assertThat(((CronTrigger) scheduler.getTrigger(key)).getCronExpression())
						.isEqualTo("0/1 * * * * ?"));

		await().atMost(Duration.ofSeconds(5)).until(() -> output.getOut().contains("Hello:"));
	}
}
