package com.example.demo.lesson;

import static com.example.demo.lesson.SchedulerTestSupport.scheduler;
import static com.example.demo.lesson.SchedulerTestSupport.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.example.demo.lesson.SchedulerTestSupport.StartTimes;

/**
 * 第 1 課：fixedRate vs fixedDelay —— 任務執行得比間隔還久時，差別才看得出來。
 *
 * <pre>
 * \@Scheduled(fixedDelay = 100)   上一次「結束」後等 100ms 再開始 → 開始間隔 = 執行時間 + 100ms
 * \@Scheduled(fixedRate = 100)    每 100ms「開始」一次；上一次還沒結束時，等它結束後立刻開始（不會同時執行兩次）
 * \@Scheduled(initialDelay = …)   應用程式啟動後，第一次執行前先等多久
 * </pre>
 *
 * 這裡的任務每次執行 200ms，間隔 100ms。
 */
class L01_FixedRateVsFixedDelayTest {

	private static final Duration TASK_DURATION = Duration.ofMillis(200);
	private static final Duration INTERVAL = Duration.ofMillis(100);

	private final ThreadPoolTaskScheduler scheduler = scheduler(1);
	private final StartTimes startTimes = new StartTimes();

	@AfterEach
	void shutdown() {
		scheduler.shutdown();
	}

	private Runnable slowTask() {
		return () -> {
			startTimes.record();
			sleep(TASK_DURATION.toMillis());
		};
	}

	@Test
	void fixedDelay_shouldWaitAfterEachRunEnds() {
		scheduler.scheduleWithFixedDelay(slowTask(), INTERVAL);

		await().atMost(Duration.ofSeconds(5)).until(() -> startTimes.count() >= 4);

		// 200ms 執行 + 100ms 等待 = 每 300ms 開始一次
		assertThat(startTimes.gaps()).allSatisfy(gap -> assertThat(gap).isGreaterThanOrEqualTo(290));
	}

	@Test
	void fixedRate_shouldStartNextRunRightAfterAnOverrunningOne() {
		scheduler.scheduleAtFixedRate(slowTask(), INTERVAL);

		await().atMost(Duration.ofSeconds(5)).until(() -> startTimes.count() >= 4);

		// 間隔是 100ms，但每次執行 200ms：下一次在上一次結束後立刻開始，開始間隔約 200ms，沒有額外等待
		assertThat(startTimes.gaps()).allSatisfy(gap -> assertThat(gap).isBetween(190L, 280L));
	}

	// fixedRate 不會讓同一個任務同時執行兩次：落後的次數會在後面補上，但一次只跑一個
	@Test
	void fixedRate_shouldNeverRunTheSameTaskConcurrently() {
		var running = new AtomicInteger();
		var maxConcurrent = new AtomicInteger();
		var runs = new AtomicInteger();
		scheduler.scheduleAtFixedRate(() -> {
			maxConcurrent.accumulateAndGet(running.incrementAndGet(), Math::max);
			sleep(TASK_DURATION.toMillis());
			running.decrementAndGet();
			runs.incrementAndGet();
		}, INTERVAL);

		await().atMost(Duration.ofSeconds(5)).until(() -> runs.get() >= 4);

		assertThat(maxConcurrent).hasValue(1);
	}

	@Test
	void initialDelay_shouldPostponeTheFirstRun() {
		scheduler.scheduleAtFixedRate(startTimes::record, Instant.now().plusMillis(300), INTERVAL);

		await().atMost(Duration.ofSeconds(5)).until(() -> startTimes.count() >= 1);

		assertThat(startTimes.starts().getFirst()).isGreaterThanOrEqualTo(290);
	}
}
