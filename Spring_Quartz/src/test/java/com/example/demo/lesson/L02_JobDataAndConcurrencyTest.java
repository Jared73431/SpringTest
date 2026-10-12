package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

/**
 * 第 2 課：JobDataMap 與並行執行。
 *
 * <ul>
 * <li>JobDataMap：JobDetail 與 Trigger 都可以帶資料，執行時合併（Trigger 的值優先）</li>
 * <li>Quartz 預設允許同一個 Job 同時執行多次（和 @Scheduled 不同）；@DisallowConcurrentExecution 禁止</li>
 * <li>@PersistJobDataAfterExecution：執行結束後把修改過的 JobDataMap 存回去（例如累計次數）</li>
 * </ul>
 *
 * 在 Spring 中，Job 還可以直接注入 Spring Bean（見 ReminderJob 與第 5 課）。
 */
class L02_JobDataAndConcurrencyTest {

	static final List<String> MESSAGES = new CopyOnWriteArrayList<>();
	static final AtomicInteger RUNNING = new AtomicInteger();
	static final AtomicInteger MAX_CONCURRENT = new AtomicInteger();
	static final List<Integer> COUNTS = new CopyOnWriteArrayList<>();

	private Scheduler scheduler;

	@BeforeEach
	void start() throws Exception {
		MESSAGES.clear();
		COUNTS.clear();
		RUNNING.set(0);
		MAX_CONCURRENT.set(0);
		scheduler = LessonSchedulers.ram("lesson-2", 3, 60_000);
		scheduler.start();
	}

	@AfterEach
	void shutdown() throws Exception {
		scheduler.shutdown(true);
	}

	public static class GreetingJob implements Job {
		@Override
		public void execute(JobExecutionContext context) {
			var data = context.getMergedJobDataMap(); // JobDetail + Trigger 的資料
			MESSAGES.add(data.getString("greeting") + ", " + data.getString("name"));
		}
	}

	/** 每次執行 300ms，用來觀察是否同時執行 */
	public static class SlowJob implements Job {
		@Override
		public void execute(JobExecutionContext context) {
			MAX_CONCURRENT.accumulateAndGet(RUNNING.incrementAndGet(), Math::max);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			RUNNING.decrementAndGet();
		}
	}

	@DisallowConcurrentExecution
	public static class SlowNonConcurrentJob extends SlowJob {
	}

	@PersistJobDataAfterExecution
	@DisallowConcurrentExecution
	public static class CountingJob implements Job {
		@Override
		public void execute(JobExecutionContext context) {
			var data = context.getJobDetail().getJobDataMap();
			int count = data.getInt("count") + 1;
			data.put("count", count);
			COUNTS.add(count);
		}
	}

	@Test
	void mergedJobDataMap_shouldPreferTriggerValues() throws Exception {
		var job = JobBuilder.newJob(GreetingJob.class)
				.usingJobData("greeting", "Hello")
				.usingJobData("name", "JobDetail")
				.build();
		var trigger = TriggerBuilder.newTrigger().startNow().usingJobData("name", "Trigger").build();

		scheduler.scheduleJob(job, trigger);

		await().atMost(Duration.ofSeconds(5)).until(() -> !MESSAGES.isEmpty());
		assertThat(MESSAGES).containsExactly("Hello, Trigger");
	}

	private void runEvery50ms(Class<? extends Job> jobClass) throws Exception {
		scheduler.scheduleJob(JobBuilder.newJob(jobClass).build(), TriggerBuilder.newTrigger().startNow()
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(50).withRepeatCount(5))
				.build());
	}

	// 預設：上一次還沒結束，下一次就開始了（有多條執行緒時會同時執行）
	@Test
	void job_shouldRunConcurrently_byDefault() throws Exception {
		runEvery50ms(SlowJob.class);

		await().atMost(Duration.ofSeconds(5)).until(() -> MAX_CONCURRENT.get() >= 2);
	}

	@Test
	void disallowConcurrentExecution_shouldRunOneAtATime() throws Exception {
		runEvery50ms(SlowNonConcurrentJob.class);

		await().atMost(Duration.ofSeconds(5)).pollDelay(Duration.ofMillis(900)).until(() -> RUNNING.get() == 0);
		assertThat(MAX_CONCURRENT).hasValue(1);
	}

	@Test
	void persistJobDataAfterExecution_shouldKeepStateBetweenRuns() throws Exception {
		scheduler.scheduleJob(JobBuilder.newJob(CountingJob.class).withIdentity("counter").usingJobData("count", 0).build(),
				TriggerBuilder.newTrigger().startNow()
						.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(50).withRepeatCount(2))
						.build());

		await().atMost(Duration.ofSeconds(5)).until(() -> COUNTS.size() == 3);
		assertThat(COUNTS).containsExactly(1, 2, 3);
		assertThat(scheduler.getJobDetail(JobKey.jobKey("counter"))).isNull(); // 不是 durable：Trigger 結束後 Job 也被移除
	}
}
