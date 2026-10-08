package com.example.demo.lesson;

import static com.example.demo.lesson.SchedulerTestSupport.scheduler;
import static com.example.demo.lesson.SchedulerTestSupport.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * 第 4 課：任務丟出例外時，下一次還會不會執行？
 *
 * <pre>
 * Spring 的排程器（@Scheduled）    記錄錯誤（ErrorHandler），下一次照常執行
 * JDK 的 ScheduledExecutorService   一次例外之後，後續的執行全部被取消，而且沒有任何 log
 * </pre>
 *
 * 所以自己用 Executors.newScheduledThreadPool 寫排程時，任務內一定要 try / catch。
 */
class L04_ExceptionTest {

	/** 第 2 次執行時丟出例外 */
	private static Runnable failingOnSecondRun(AtomicInteger runs) {
		return () -> {
			if (runs.incrementAndGet() == 2) {
				throw new IllegalStateException("第 2 次執行失敗");
			}
		};
	}

	@Test
	void springScheduler_shouldKeepRunning_afterAnException() {
		var scheduler = scheduler(1);
		var runs = new AtomicInteger();
		List<Throwable> errors = new CopyOnWriteArrayList<>();
		scheduler.setErrorHandler(errors::add); // 預設是記錄 log；也可以自訂，例如發送告警
		try {
			scheduler.scheduleAtFixedRate(failingOnSecondRun(runs), Duration.ofMillis(50));

			await().atMost(Duration.ofSeconds(5)).until(() -> runs.get() >= 5);

			assertThat(errors).singleElement().satisfies(e -> assertThat(e).hasMessage("第 2 次執行失敗"));
		} finally {
			scheduler.shutdown();
		}
	}

	@Test
	void jdkScheduledExecutor_shouldStopForever_afterAnException() {
		var executor = Executors.newSingleThreadScheduledExecutor();
		var runs = new AtomicInteger();
		try {
			executor.scheduleAtFixedRate(failingOnSecondRun(runs), 0, 50, TimeUnit.MILLISECONDS);

			await().atMost(Duration.ofSeconds(5)).until(() -> runs.get() >= 2);
			sleep(300); // 本來還應該再執行約 6 次

			assertThat(runs).hasValue(2);
		} finally {
			executor.shutdownNow();
		}
	}
}
