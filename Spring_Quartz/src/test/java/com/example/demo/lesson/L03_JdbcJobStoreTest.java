package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.lesson.LessonSchedulers.RecordingJob;

/**
 * 第 3 課：JDBC JobStore —— 排程存在資料庫，應用程式重新啟動後仍然存在。
 *
 * <pre>
 * RAMJobStore（預設）  排程只在記憶體中：重新啟動後，執行中用 API 新增的排程全部消失
 * JDBC JobStore        排程存在 qrtz_* 資料表：重新啟動後繼續；也是叢集（第 6 課）的前提
 * </pre>
 *
 * 用「關閉 Scheduler 再建立一個新的」模擬應用程式重新啟動。
 */
@SpringBootTest
class L03_JdbcJobStoreTest extends PostgresContainerTestBase {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static void scheduleNightlyJob(Scheduler scheduler) throws Exception {
		scheduler.scheduleJob(
				JobBuilder.newJob(RecordingJob.class).withIdentity("nightly-inventory").build(),
				TriggerBuilder.newTrigger().withIdentity("nightly-inventory")
						.withSchedule(CronScheduleBuilder.cronSchedule("0 30 22 * * ?")).build());
	}

	@Test
	void ramJobStore_shouldLoseJobs_afterRestart() throws Exception {
		Scheduler first = LessonSchedulers.ram("lesson-3-ram", 1, 60_000);
		scheduleNightlyJob(first);
		first.shutdown();

		Scheduler restarted = LessonSchedulers.ram("lesson-3-ram", 1, 60_000);
		try {
			assertThat(restarted.checkExists(JobKey.jobKey("nightly-inventory"))).isFalse();
		} finally {
			restarted.shutdown();
		}
	}

	@Test
	void jdbcJobStore_shouldKeepJobs_afterRestart() throws Exception {
		Scheduler first = LessonSchedulers.jdbcNode("lesson-3-jdbc", dataSource, false);
		first.clear();
		scheduleNightlyJob(first);
		first.shutdown();

		Scheduler restarted = LessonSchedulers.jdbcNode("lesson-3-jdbc", dataSource, false);
		try {
			assertThat(restarted.checkExists(JobKey.jobKey("nightly-inventory"))).isTrue();
			assertThat(restarted.getTrigger(TriggerKey.triggerKey("nightly-inventory")).getNextFireTime()).isNotNull();
		} finally {
			restarted.clear();
			restarted.shutdown();
		}
	}

	// 排程就是資料表中的資料：SCHED_NAME 區分不同的 Scheduler，同一個資料庫可以放多組排程
	@Test
	void jobsAndTriggers_shouldBeRowsInQuartzTables() throws Exception {
		Scheduler scheduler = LessonSchedulers.jdbcNode("lesson-3-rows", dataSource, false);
		try {
			scheduler.clear();
			scheduleNightlyJob(scheduler);

			assertThat(jdbcTemplate.queryForObject(
					"SELECT cron_expression FROM qrtz_cron_triggers WHERE sched_name = 'lesson-3-rows'", String.class))
					.isEqualTo("0 30 22 * * ?");
			assertThat(jdbcTemplate.queryForObject(
					"SELECT job_class_name FROM qrtz_job_details WHERE sched_name = 'lesson-3-rows'", String.class))
					.isEqualTo(RecordingJob.class.getName());
		} finally {
			scheduler.clear();
			scheduler.shutdown();
		}
	}
}
