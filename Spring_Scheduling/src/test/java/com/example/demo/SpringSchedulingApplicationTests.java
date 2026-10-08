package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * Baseline：鎖定重構前註冊的排程。
 */
@SpringBootTest
class SpringSchedulingApplicationTests {

	@Autowired
	private ScheduledTaskHolder scheduledTaskHolder;

	private Set<String> describe() {
		return scheduledTaskHolder.getScheduledTasks().stream().map(ScheduledTask::getTask).map(task -> {
			if (task instanceof CronTask cron) {
				return "cron " + cron.getExpression();
			}
			if (task instanceof FixedRateTask rate) {
				return "fixedRate " + rate.getInterval();
			}
			if (task instanceof FixedDelayTask delay) {
				return "fixedDelay " + delay.getInterval();
			}
			return task.toString();
		}).collect(Collectors.toSet());
	}

	// [Potential Bug] fixedDelay 上的 zone = "timeZone"：zone 只對 cron 有效，而且 "timeZone" 是字串、不是欄位，設定沒有作用
	@Test
	void scheduledTasks_shouldBeRegistered() {
		assertThat(describe()).containsExactlyInAnyOrder("fixedDelay 60000", "fixedRate 1000", "cron */1 * * * * *");
	}
}
