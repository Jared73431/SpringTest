package com.example.demo.lesson;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 課程共用：建立排程器、記錄每次執行的開始時間。
 *
 * <p>
 * \@Scheduled 背後就是 ThreadPoolTaskScheduler：fixedRate → scheduleAtFixedRate、fixedDelay → scheduleWithFixedDelay、
 * cron → schedule(task, CronTrigger)。直接使用它，課程不需要啟動 Spring，間隔也可以設得很短。
 */
final class SchedulerTestSupport {

	private SchedulerTestSupport() {
	}

	static ThreadPoolTaskScheduler scheduler(int poolSize) {
		var scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(poolSize);
		scheduler.setThreadNamePrefix("lesson-");
		scheduler.initialize();
		return scheduler;
	}

	static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** 記錄每次開始執行的時間（毫秒） */
	static final class StartTimes {

		private final long origin = System.nanoTime();
		private final List<Long> starts = new CopyOnWriteArrayList<>();

		void record() {
			starts.add((System.nanoTime() - origin) / 1_000_000);
		}

		int count() {
			return starts.size();
		}

		List<Long> starts() {
			return List.copyOf(starts);
		}

		/** 相鄰兩次開始時間的間隔 */
		List<Long> gaps() {
			var snapshot = starts();
			return java.util.stream.IntStream.range(1, snapshot.size())
					.mapToObj(i -> snapshot.get(i) - snapshot.get(i - 1))
					.toList();
		}
	}
}
