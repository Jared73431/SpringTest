package com.example.demo.reminder;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

/**
 * 動態排程：執行中新增、修改、暫停、刪除提醒，排程存在資料庫（JDBC JobStore），重新啟動後仍然存在。
 *
 * <p>
 * 這就是「每家門市各自一個排程」的做法：門市新增或調整打烊時間時，呼叫 create / reschedule 即可，不需要重新部署。
 */
@Service
public class ReminderService {

	static final String GROUP = "reminders";
	static final String DEFAULT_TIME_ZONE = "Asia/Taipei";

	private final Scheduler scheduler;

	public ReminderService(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public ReminderResponse create(ReminderRequest request) {
		TimeZone timeZone = timeZone(request.timeZone());
		validateCron(request.cron());
		JobKey jobKey = JobKey.jobKey(request.name(), GROUP);
		try {
			if (scheduler.checkExists(jobKey)) {
				throw new DuplicateReminderException(request.name());
			}
			JobDetail job = JobBuilder.newJob(ReminderJob.class)
					.withIdentity(jobKey)
					.usingJobData(ReminderJob.RECIPIENT, request.recipient())
					.usingJobData(ReminderJob.MESSAGE, request.message())
					.build();
			scheduler.scheduleJob(job, cronTrigger(request.name(), request.cron(), timeZone));
			return find(request.name());
		} catch (SchedulerException e) {
			throw new IllegalStateException("新增提醒失敗：" + request.name(), e);
		}
	}

	public List<ReminderResponse> findAll() {
		try {
			return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(GROUP)).stream()
					.map(key -> find(key.getName()))
					.sorted((a, b) -> a.name().compareTo(b.name()))
					.toList();
		} catch (SchedulerException e) {
			throw new IllegalStateException("查詢提醒失敗", e);
		}
	}

	public ReminderResponse find(String name) {
		try {
			JobDetail job = scheduler.getJobDetail(JobKey.jobKey(name, GROUP));
			CronTrigger trigger = (CronTrigger) scheduler.getTrigger(TriggerKey.triggerKey(name, GROUP));
			if (job == null || trigger == null) {
				throw new ReminderNotFoundException(name);
			}
			return new ReminderResponse(name, trigger.getCronExpression(), trigger.getTimeZone().getID(),
					job.getJobDataMap().getString(ReminderJob.RECIPIENT), job.getJobDataMap().getString(ReminderJob.MESSAGE),
					scheduler.getTriggerState(trigger.getKey()).name(), toInstant(trigger.getNextFireTime()),
					toInstant(trigger.getPreviousFireTime()));
		} catch (SchedulerException e) {
			throw new IllegalStateException("查詢提醒失敗：" + name, e);
		}
	}

	// 修改觸發時間：用新的 Trigger 取代舊的（同一個 TriggerKey），Job 與它的資料不變
	public ReminderResponse reschedule(String name, RescheduleRequest request) {
		TimeZone timeZone = timeZone(request.timeZone());
		validateCron(request.cron());
		find(name);
		try {
			scheduler.rescheduleJob(TriggerKey.triggerKey(name, GROUP), cronTrigger(name, request.cron(), timeZone));
			return find(name);
		} catch (SchedulerException e) {
			throw new IllegalStateException("修改提醒失敗：" + name, e);
		}
	}

	// 暫停：Trigger 不再觸發，但保留在資料庫中（修正前用 shutdown()，那是永久關閉整個 Scheduler，之後無法再 start）
	public void pause(String name) {
		find(name);
		try {
			scheduler.pauseJob(JobKey.jobKey(name, GROUP));
		} catch (SchedulerException e) {
			throw new IllegalStateException("暫停提醒失敗：" + name, e);
		}
	}

	public void resume(String name) {
		find(name);
		try {
			scheduler.resumeJob(JobKey.jobKey(name, GROUP));
		} catch (SchedulerException e) {
			throw new IllegalStateException("恢復提醒失敗：" + name, e);
		}
	}

	// 立刻執行一次（不影響原本的排程），例如補做某一次
	public void runNow(String name) {
		find(name);
		try {
			scheduler.triggerJob(JobKey.jobKey(name, GROUP));
		} catch (SchedulerException e) {
			throw new IllegalStateException("執行提醒失敗：" + name, e);
		}
	}

	public void delete(String name) {
		find(name);
		try {
			scheduler.deleteJob(JobKey.jobKey(name, GROUP));
		} catch (SchedulerException e) {
			throw new IllegalStateException("刪除提醒失敗：" + name, e);
		}
	}

	/**
	 * Misfire（錯過觸發時間，例如應用程式停機）時的處理：FireAndProceed = 恢復後立刻補做「一次」，之後照原本的排程。
	 * 其他選擇見第 4 課。
	 */
	private static Trigger cronTrigger(String name, String cron, TimeZone timeZone) {
		return TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.triggerKey(name, GROUP))
				.forJob(JobKey.jobKey(name, GROUP))
				.withSchedule(CronScheduleBuilder.cronSchedule(cron)
						.inTimeZone(timeZone)
						.withMisfireHandlingInstructionFireAndProceed())
				.build();
	}

	private static void validateCron(String cron) {
		if (!CronExpression.isValidExpression(cron)) {
			throw new InvalidScheduleException("cron 格式不正確：" + cron);
		}
	}

	private static TimeZone timeZone(String zone) {
		try {
			return TimeZone.getTimeZone(ZoneId.of(zone == null || zone.isBlank() ? DEFAULT_TIME_ZONE : zone));
		} catch (DateTimeException e) {
			throw new InvalidScheduleException("時區不正確：" + zone);
		}
	}

	private static java.time.Instant toInstant(Date date) {
		return date == null ? null : date.toInstant();
	}
}
