package com.example.demo.lesson;

import static com.example.demo.lesson.LessonSchedulers.EXECUTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.example.demo.lesson.LessonSchedulers.Execution;
import com.example.demo.lesson.LessonSchedulers.RecordingJob;

/**
 * 第 1 課：Job、JobDetail、Trigger。
 *
 * <pre>
 * Job        要做的事（一個類別，每次觸發都會建立新的實例）
 * JobDetail  一個「工作」：Job 類別 + 名稱 + 資料（JobDataMap）
 * Trigger    什麼時候觸發：SimpleTrigger（每隔多久、重複幾次）或 CronTrigger（cron 運算式）
 * </pre>
 *
 * 一個 JobDetail 可以有多個 Trigger；和 @Scheduled 不同，Job 與 Trigger 可以在執行中新增、修改、刪除（第 5 課）。
 */
class L01_QuartzBasicsTest {

	private Scheduler scheduler;

	@BeforeEach
	void start() throws Exception {
		EXECUTIONS.clear();
		scheduler = LessonSchedulers.ram("lesson-1", 2, 60_000);
		scheduler.start();
	}

	@AfterEach
	void shutdown() throws Exception {
		scheduler.shutdown(true);
	}

	// repeatCount = 2 代表「重複 2 次」：總共執行 3 次。執行完畢的 Trigger 會被移除
	@Test
	void simpleTrigger_shouldFireRepeatCountPlusOneTimes() throws Exception {
		JobDetail job = JobBuilder.newJob(RecordingJob.class).withIdentity("simple").build();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("every-50ms").startNow()
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(50).withRepeatCount(2))
				.build();

		scheduler.scheduleJob(job, trigger);

		await().atMost(Duration.ofSeconds(5)).until(() -> scheduler.getTrigger(TriggerKey.triggerKey("every-50ms")) == null);
		assertThat(LessonSchedulers.count("simple")).isEqualTo(3);
	}

	// 一個 JobDetail、兩個 Trigger：例如同一份報表，平日與週末各有不同的時間
	@Test
	void oneJob_canHaveManyTriggers() throws Exception {
		JobDetail job = JobBuilder.newJob(RecordingJob.class).withIdentity("report").storeDurably().build();
		scheduler.addJob(job, false);
		for (String name : new String[] { "trigger-a", "trigger-b" }) {
			scheduler.scheduleJob(TriggerBuilder.newTrigger().withIdentity(name).forJob(job).startNow().build());
		}

		await().atMost(Duration.ofSeconds(5)).until(() -> LessonSchedulers.count("report") == 2);
		assertThat(EXECUTIONS).extracting(Execution::trigger).containsExactlyInAnyOrder("trigger-a", "trigger-b");
	}

	// Quartz 的 cron：秒 分 時 日 月 星期 [年]；「日」與「星期」其中一個必須是 ?
	@Test
	void quartzCron_requiresQuestionMarkInDayOfMonthOrDayOfWeek() {
		assertThat(CronExpression.isValidExpression("0 30 22 * * ?")).isTrue();
		assertThat(CronExpression.isValidExpression("0 30 22 * * *")).isFalse(); // Spring @Scheduled 可以這樣寫，Quartz 不行
	}

	// 陷阱：星期的數字。Quartz 是 1 = 星期日、2 = 星期一；Spring @Scheduled 是 1 = 星期一。用英文縮寫（MON）最安全
	@Test
	void dayOfWeekNumbers_shouldDifferFromSpringCron() throws Exception {
		ZonedDateTime sundayNoon = ZonedDateTime.of(2026, 10, 11, 12, 0, 0, 0, ZoneId.of("Asia/Taipei"));

		Date quartzTwo = nextAfter("0 0 9 ? * 2", sundayNoon);
		ZonedDateTime springTwo = org.springframework.scheduling.support.CronExpression.parse("0 0 9 * * 2").next(sundayNoon);

		assertThat(quartzTwo.toInstant().atZone(sundayNoon.getZone()).getDayOfWeek()).hasToString("MONDAY");
		assertThat(springTwo.getDayOfWeek()).hasToString("TUESDAY");
	}

	// Quartz 特有的語法：L（最後）、W（最接近的平日）、#（第幾個星期幾）
	@Test
	void quartzCron_shouldSupportNthWeekdayOfMonth() throws Exception {
		ZonedDateTime start = ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Asia/Taipei"));

		// 6#3 = 每月第 3 個星期五（Quartz：6 = 星期五）
		Date next = nextAfter("0 0 9 ? * 6#3", start);

		assertThat(next.toInstant().atZone(start.getZone()).toLocalDate()).hasToString("2026-10-16");
	}

	private static Date nextAfter(String cron, ZonedDateTime after) throws Exception {
		var expression = new CronExpression(cron);
		expression.setTimeZone(TimeZone.getTimeZone(after.getZone()));
		return expression.getNextValidTimeAfter(Date.from(after.toInstant()));
	}
}
