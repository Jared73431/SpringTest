package com.example.demo.lesson;

import static com.example.demo.lesson.SchedulerTestSupport.sleep;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.PostgresContainerTestBase;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

/**
 * 第 6 課：部署多台主機時，同一個排程只讓一台執行（ShedLock）。
 *
 * <p>
 * \@Scheduled 只管自己這個程序：部署 2 台，每天凌晨 3 點的清理就會執行 2 次。ShedLock 在執行前先到資料庫的 shedlock 表
 * 「搶鎖」，搶到的主機才執行，沒搶到的直接跳過（不會排隊等待）。
 *
 * <pre>
 * lockAtMostFor    鎖最多保留多久：執行到一半主機當機時，過了這段時間鎖會自動失效，其他主機才能再執行
 * lockAtLeastFor   鎖至少保留多久：任務很快就結束時，避免時鐘稍有誤差的其他主機在同一個排程時間點又執行一次
 * </pre>
 *
 * 實際的排程只要加上 @SchedulerLock(name = ...)；這裡直接使用 LockingTaskExecutor，模擬多台主機同時執行。
 */
@SpringBootTest
class L06_ShedLockTest extends PostgresContainerTestBase {

	@Autowired
	private LockProvider lockProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private LockingTaskExecutor executor() {
		return new DefaultLockingTaskExecutor(lockProvider);
	}

	private static LockConfiguration lock(String name, Duration atMost, Duration atLeast) {
		return new LockConfiguration(Instant.now(), name, atMost, atLeast);
	}

	// 兩台「主機」在同一個時間點觸發同一個排程：只有一台執行
	@Test
	void concurrentRuns_shouldExecuteOnlyOnce() throws Exception {
		var runs = new AtomicInteger();
		var bothStarted = new CountDownLatch(2);
		Runnable task = () -> {
			runs.incrementAndGet();
			sleep(500);
		};

		Runnable node = () -> {
			bothStarted.countDown();
			try {
				bothStarted.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			executor().executeWithLock(task, lock("concurrent-report", Duration.ofSeconds(10), Duration.ZERO));
		};
		CompletableFuture.allOf(CompletableFuture.runAsync(node), CompletableFuture.runAsync(node)).get();

		assertThat(runs).hasValue(1);
	}

	// 任務很快就結束：lockAtLeastFor 讓鎖繼續保留，稍晚觸發的另一台主機也不會再執行一次
	@Test
	void lockAtLeastFor_shouldPreventARerunRightAfterTheTaskFinishes() {
		var runs = new AtomicInteger();

		executor().executeWithLock((Runnable) runs::incrementAndGet, lock("at-least", Duration.ofSeconds(10), Duration.ofSeconds(5)));
		executor().executeWithLock((Runnable) runs::incrementAndGet, lock("at-least", Duration.ofSeconds(10), Duration.ofSeconds(5)));

		assertThat(runs).hasValue(1);
	}

	// 沒有 lockAtLeastFor 時，任務結束就釋放鎖，下一次可以執行
	@Test
	void lock_shouldBeReleasedAfterTheTask_whenLockAtLeastForIsZero() {
		var runs = new AtomicInteger();

		executor().executeWithLock((Runnable) runs::incrementAndGet, lock("released", Duration.ofSeconds(10), Duration.ZERO));
		executor().executeWithLock((Runnable) runs::incrementAndGet, lock("released", Duration.ofSeconds(10), Duration.ZERO));

		assertThat(runs).hasValue(2);
	}

	// 鎖就是 shedlock 表中的一列：記錄持有者（主機名稱）與到期時間
	@Test
	void lock_shouldBeStoredInTheShedlockTable() {
		executor().executeWithLock((Runnable) () -> {
		}, lock("table-row", Duration.ofSeconds(10), Duration.ofSeconds(5)));

		assertThat(jdbcTemplate.queryForObject("SELECT locked_by FROM shedlock WHERE name = 'table-row'", String.class))
				.isNotBlank();
	}
}
