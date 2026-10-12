package com.example.demo.lesson;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * 課程共用：建立獨立的 Scheduler，並記錄每次執行。
 */
final class LessonSchedulers {

	private LessonSchedulers() {
	}

	/** 記憶體 JobStore（RAMJobStore）：排程只存在記憶體中，應用程式停止就消失 */
	static Scheduler ram(String name, int threadCount, long misfireThresholdMillis) throws SchedulerException {
		var properties = new Properties();
		properties.setProperty("org.quartz.scheduler.instanceName", name);
		properties.setProperty("org.quartz.threadPool.threadCount", String.valueOf(threadCount));
		properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
		properties.setProperty("org.quartz.jobStore.misfireThreshold", String.valueOf(misfireThresholdMillis));
		return new StdSchedulerFactory(properties).getScheduler();
	}

	/**
	 * JDBC JobStore 的一個「節點」：使用 Spring 的 DataSource（和 spring.quartz.job-store-type=jdbc 相同的做法）。
	 * 同一個 schedulerName 代表同一組排程；clustered = true 時多個節點共用並互相接手。
	 */
	static Scheduler jdbcNode(String schedulerName, DataSource dataSource, boolean clustered) throws Exception {
		var properties = new Properties();
		properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
		properties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
		properties.setProperty("org.quartz.jobStore.isClustered", String.valueOf(clustered));
		properties.setProperty("org.quartz.jobStore.clusterCheckinInterval", "1000");
		properties.setProperty("org.quartz.threadPool.threadCount", "3");

		var factory = new SchedulerFactoryBean();
		factory.setSchedulerName(schedulerName);
		factory.setDataSource(dataSource);
		factory.setQuartzProperties(properties);
		factory.setAutoStartup(false);
		factory.afterPropertiesSet();
		return factory.getScheduler();
	}

	/** 一次執行的紀錄 */
	record Execution(String job, String trigger, long scheduledFireTime, String instanceId, long startedAt) {
	}

	/** 所有課程 Job 共用的執行紀錄（Quartz 每次觸發都建立新的 Job 實例，所以用 static 收集） */
	static final List<Execution> EXECUTIONS = new CopyOnWriteArrayList<>();

	static void record(JobExecutionContext context) {
		String instanceId;
		try {
			instanceId = context.getScheduler().getSchedulerInstanceId();
		} catch (SchedulerException e) {
			instanceId = "unknown";
		}
		EXECUTIONS.add(new Execution(context.getJobDetail().getKey().getName(), context.getTrigger().getKey().getName(),
				context.getScheduledFireTime().getTime(), instanceId, System.currentTimeMillis()));
	}

	static long count(String job) {
		return EXECUTIONS.stream().filter(e -> e.job().equals(job)).count();
	}

	/** 只記錄執行的 Job */
	public static class RecordingJob implements Job {
		@Override
		public void execute(JobExecutionContext context) {
			record(context);
		}
	}
}
