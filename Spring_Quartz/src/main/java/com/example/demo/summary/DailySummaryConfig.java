package com.example.demo.summary;

import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 固定的排程：把 JobDetail 與 Trigger 宣告成 Bean，Spring Boot 會自動註冊到 Scheduler
 * （spring.quartz.overwrite-existing-jobs=true：程式中的設定覆蓋資料庫中的舊設定）。
 *
 * <p>
 * 修正前的 Spring_Quartz 自己建立 SchedulerFactoryBean，並用 MethodInvokingJobDetailFactoryBean 呼叫任意方法；
 * 那種 JobDetail 無法序列化，不能用在 JDBC JobStore。Spring_Quartz-2 用的就是這裡的 Bean 寫法。
 */
@Configuration(proxyBeanMethods = false)
public class DailySummaryConfig {

	@Bean
	JobDetail dailySummaryJob() {
		return JobBuilder.newJob(DailySummaryJob.class)
				.withIdentity("daily-summary", "system")
				// 沒有 Trigger 時也保留這個 JobDetail（Trigger 是另一個 Bean，註冊順序不一定）
				.storeDurably()
				.build();
	}

	@Bean
	Trigger dailySummaryTrigger(JobDetail dailySummaryJob) {
		return TriggerBuilder.newTrigger()
				.forJob(dailySummaryJob)
				.withIdentity("daily-summary", "system")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 23 * * ?").inTimeZone(TimeZone.getTimeZone("Asia/Taipei")))
				.build();
	}
}
