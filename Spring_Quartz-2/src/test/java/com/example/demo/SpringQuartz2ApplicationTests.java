package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Baseline：鎖定合併前 Spring_Quartz-2 的行為。
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class SpringQuartz2ApplicationTests {

	@Autowired
	private Scheduler scheduler;

	@Test
	void jobDetailAndTriggerBeans_shouldBeRegisteredByBoot() throws Exception {
		assertThat(scheduler.getJobKeys(GroupMatcher.anyGroup())).hasSize(2);
		assertThat(scheduler.getTriggerKeys(GroupMatcher.anyGroup())).hasSize(2);
	}

	@Test
	void quartzJob1_shouldRun(CapturedOutput output) {
		await().atMost(Duration.ofSeconds(5)).until(() -> output.getOut().contains("QuartzJob1----"));
	}

	// 匿名內部類別寫的 jobDetail2 也會執行：Boot 的 SpringBeanJobFactory 用 createBean() 建立 Job，
	// 匿名類別建構子需要的外部類別 QuartzConfig 剛好是 Spring Bean，所以能注入成功（能運作，但很脆弱）
	@Test
	void anonymousInnerClassJob_shouldRun(CapturedOutput output) {
		await().atMost(Duration.ofSeconds(5)).until(() -> output.getOut().contains("內部類quartzJob2----"));
	}
}
