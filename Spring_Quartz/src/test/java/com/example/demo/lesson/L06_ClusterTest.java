package com.example.demo.lesson;

import static com.example.demo.lesson.LessonSchedulers.EXECUTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.lesson.LessonSchedulers.Execution;
import com.example.demo.lesson.LessonSchedulers.RecordingJob;

/**
 * 第 6 課：叢集 —— 多台主機共用同一個資料庫。
 *
 * <ul>
 * <li>每一次觸發只有一台執行：節點從資料庫「取得」下一個要觸發的 Trigger 時會加鎖（qrtz_locks），不會重複</li>
 * <li>某一台停止或當機：其他台每隔 clusterCheckinInterval 檢查，接手它的排程</li>
 * </ul>
 *
 * 和 ShedLock（Spring_Scheduling 第 6 課）的差別：ShedLock 是「每台都觸發、搶到鎖的執行」；Quartz 叢集是
 * 「只有一台會被分配到這次觸發」，而且排程本身存在資料庫，可以動態修改。
 */
@SpringBootTest
class L06_ClusterTest extends PostgresContainerTestBase {

	@Autowired
	private DataSource dataSource;

	private Scheduler nodeA;
	private Scheduler nodeB;

	@BeforeEach
	void startTwoNodes() throws Exception {
		EXECUTIONS.clear();
		nodeA = LessonSchedulers.jdbcNode("lesson-6-cluster", dataSource, true);
		nodeB = LessonSchedulers.jdbcNode("lesson-6-cluster", dataSource, true);
		nodeA.clear();
		nodeA.start();
		nodeB.start();
		// 每 200ms 觸發一次：只排在其中一個節點上，兩個節點共用
		nodeA.scheduleJob(JobBuilder.newJob(RecordingJob.class).withIdentity("cluster-job").build(),
				TriggerBuilder.newTrigger().startNow()
						.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(200).repeatForever())
						.build());
	}

	@AfterEach
	void shutdownNodes() throws Exception {
		if (!nodeA.isShutdown()) {
			nodeA.clear();
			nodeA.shutdown(true);
		}
		if (!nodeB.isShutdown()) {
			nodeB.clear();
			nodeB.shutdown(true);
		}
	}

	@Test
	void nodes_shouldHaveDifferentInstanceIds() throws Exception {
		assertThat(nodeA.getSchedulerInstanceId()).isNotEqualTo(nodeB.getSchedulerInstanceId());
	}

	// 每一次觸發（scheduledFireTime）只被執行一次
	@Test
	void eachFire_shouldRunOnExactlyOneNode() {
		await().atMost(Duration.ofSeconds(15)).until(() -> EXECUTIONS.size() >= 10);

		var fireTimes = EXECUTIONS.stream().map(Execution::scheduledFireTime).toList();
		assertThat(fireTimes).doesNotHaveDuplicates();
	}

	// 一個節點停止後，另一個節點繼續執行
	@Test
	void remainingNode_shouldTakeOver_whenOneNodeStops() throws Exception {
		await().atMost(Duration.ofSeconds(15)).until(() -> EXECUTIONS.size() >= 3);
		String stoppedNode = nodeA.getSchedulerInstanceId();
		nodeA.shutdown(true);
		int before = EXECUTIONS.size();

		await().atMost(Duration.ofSeconds(15)).until(() -> EXECUTIONS.size() >= before + 5);

		var nodesAfterStop = EXECUTIONS.subList(before, EXECUTIONS.size()).stream().map(Execution::instanceId)
				.collect(Collectors.toSet());
		assertThat(nodesAfterStop).containsExactly(nodeB.getSchedulerInstanceId()).doesNotContain(stoppedNode);
	}
}
