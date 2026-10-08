package com.example.demo.lesson;

import static com.example.demo.lesson.SchedulerTestSupport.scheduler;
import static com.example.demo.lesson.SchedulerTestSupport.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 第 3 課：所有 @Scheduled 預設共用 1 條執行緒。
 *
 * <p>
 * Spring Boot 預設的排程執行緒池大小是 1（spring.task.scheduling.pool.size）。一個任務卡住（例如查詢很慢的資料庫、
 * 呼叫沒有逾時的外部 API），其他所有排程都要等它結束才能執行。
 *
 * <pre>
 * 解法                                         說明
 * spring.task.scheduling.pool.size=4           增加執行緒（本模組的設定）
 * spring.threads.virtual.enabled=true           Java 21：每次執行使用新的 Virtual Thread，不再受執行緒數量限制
 * 慢任務改用 @Async 或 Spring Batch              排程只負責觸發，實際工作交給其他執行緒
 * </pre>
 */
class L03_SingleThreadTest {

	private static final long SLOW_TASK_MILLIS = 500;

	/** 慢任務先開始執行 500ms，同時每 50ms 有一個快速任務要執行：記錄快速任務第一次真正開始的時間 */
	private long firstFastRunAfterSlowTaskStarted(int poolSize) {
		ThreadPoolTaskScheduler scheduler = scheduler(poolSize);
		var slowStarted = new AtomicLong();
		try {
			scheduler.schedule(() -> {
				slowStarted.set(System.nanoTime());
				sleep(SLOW_TASK_MILLIS);
			}, Instant.now());
			await().until(() -> slowStarted.get() != 0);

			var fastStarted = new AtomicLong();
			scheduler.scheduleAtFixedRate(() -> fastStarted.compareAndSet(0, System.nanoTime()), Duration.ofMillis(50));
			await().atMost(Duration.ofSeconds(5)).until(() -> fastStarted.get() != 0);
			return (fastStarted.get() - slowStarted.get()) / 1_000_000;
		} finally {
			scheduler.shutdown();
		}
	}

	@Test
	void singleThread_shouldDelayOtherTasks_whileOneTaskIsSlow() {
		// 快速任務要等慢任務結束（500ms）才輪得到
		assertThat(firstFastRunAfterSlowTaskStarted(1)).isGreaterThanOrEqualTo(SLOW_TASK_MILLIS - 10);
	}

	@Test
	void multipleThreads_shouldLetOtherTasksRun_whileOneTaskIsSlow() {
		// 有第 2 條執行緒，快速任務馬上就能執行
		assertThat(firstFastRunAfterSlowTaskStarted(2)).isLessThan(200);
	}
}
