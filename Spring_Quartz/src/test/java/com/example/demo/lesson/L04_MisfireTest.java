package com.example.demo.lesson;

import static com.example.demo.lesson.LessonSchedulers.EXECUTIONS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.Scheduler;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.example.demo.lesson.LessonSchedulers.RecordingJob;

/**
 * 第 4 課：Misfire —— 停機期間錯過的觸發，恢復後要怎麼辦？
 *
 * <p>
 * 這是 Quartz 和 @Scheduled 最大的差別之一：@Scheduled 停機時錯過的執行就錯過了；Quartz 記得上一次應該觸發的時間，
 * 恢復後依照 misfire 策略決定要不要補做。錯過超過 misfireThreshold（預設 60 秒）才算 misfire。
 *
 * <pre>
 * 策略（CronTrigger）                              恢復後
 * withMisfireHandlingInstructionFireAndProceed     立刻補做「一次」，之後照原本的時間（預設；本模組的提醒使用這個）
 * withMisfireHandlingInstructionDoNothing          不補做，等下一次的時間
 * withMisfireHandlingInstructionIgnoreMisfires     錯過幾次就補做幾次（例如每小時的統計，每一次都不能少）
 * </pre>
 *
 * 模擬停機：Scheduler 先不啟動（standby），讓每秒一次的 Trigger 錯過約 3 次，再啟動，觀察啟動後 300ms 內補做了幾次。
 */
class L04_MisfireTest {

	private Scheduler scheduler;

	@BeforeEach
	void createScheduler() throws Exception {
		EXECUTIONS.clear();
		// misfireThreshold 500ms：錯過超過 0.5 秒就算 misfire（讓測試不必等 60 秒）
		scheduler = LessonSchedulers.ram("lesson-4", 5, 500);
	}

	@AfterEach
	void shutdown() throws Exception {
		scheduler.shutdown(false);
	}

	/** 排一個每秒觸發的 Trigger，停機 3.5 秒後啟動，回傳啟動後 300ms 內執行的次數 */
	private long executionsRightAfterDowntime(ScheduleBuilder<? extends Trigger> schedule) throws Exception {
		scheduler.scheduleJob(JobBuilder.newJob(RecordingJob.class).withIdentity("every-second").build(),
				TriggerBuilder.newTrigger().startAt(new Date()).withSchedule(schedule).build());

		Thread.sleep(3500); // 「停機」：Scheduler 還沒啟動，錯過約 3 次
		// 在整秒之後才啟動，避免下一次正常的觸發落在觀察的 300ms 內
		while (System.currentTimeMillis() % 1000 > 100) {
			Thread.sleep(5);
		}
		scheduler.start();
		Thread.sleep(300);
		return LessonSchedulers.count("every-second");
	}

	@Test
	void fireAndProceed_shouldCatchUpOnce() throws Exception {
		var schedule = CronScheduleBuilder.cronSchedule("* * * * * ?").withMisfireHandlingInstructionFireAndProceed();

		assertThat(executionsRightAfterDowntime(schedule)).isEqualTo(1);
	}

	@Test
	void doNothing_shouldSkipMissedFires() throws Exception {
		var schedule = CronScheduleBuilder.cronSchedule("* * * * * ?").withMisfireHandlingInstructionDoNothing();

		assertThat(executionsRightAfterDowntime(schedule)).isZero();
	}

	@Test
	void ignoreMisfires_shouldCatchUpEveryMissedFire() throws Exception {
		var schedule = CronScheduleBuilder.cronSchedule("* * * * * ?").withMisfireHandlingInstructionIgnoreMisfires();

		assertThat(executionsRightAfterDowntime(schedule)).isGreaterThanOrEqualTo(3);
	}

	// SimpleTrigger 也有自己的一組策略，例如「立刻執行，並保留剩下的次數」
	@Test
	void simpleTrigger_fireNow_shouldCatchUpOnce() throws Exception {
		var schedule = SimpleScheduleBuilder.repeatSecondlyForever().withMisfireHandlingInstructionNowWithExistingCount();

		assertThat(executionsRightAfterDowntime(schedule)).isEqualTo(1);
	}
}
