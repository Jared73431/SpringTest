package com.example.demo.reminder;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.example.demo.notification.NotificationService;

/**
 * 提醒的 Job：每一個提醒是一個 JobDetail（帶著自己的 recipient、message）+ 一個 CronTrigger。
 *
 * <ul>
 * <li>Spring Bean 注入：Spring Boot 的 SpringBeanJobFactory 用 Spring 建立 Job，建構子可以直接注入 Bean
 * （修正前另外寫了 MyJobFactory 做同一件事）</li>
 * <li>JobDataMap 的值會自動呼叫同名的 setter 注入（setRecipient、setMessage）</li>
 * <li>\@DisallowConcurrentExecution：同一個提醒上一次還沒執行完時，下一次觸發會等它結束，不會同時執行兩次</li>
 * </ul>
 *
 * Quartz 每次觸發都會建立新的 Job 實例，所以 Job 本身不能保存狀態。
 */
@DisallowConcurrentExecution
public class ReminderJob extends QuartzJobBean {

	static final String RECIPIENT = "recipient";
	static final String MESSAGE = "message";

	private final NotificationService notificationService;

	private String recipient;
	private String message;

	public ReminderJob(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) {
		notificationService.send(recipient, message, instanceId(context));
	}

	private static String instanceId(JobExecutionContext context) {
		try {
			return context.getScheduler().getSchedulerInstanceId();
		} catch (SchedulerException e) {
			return "unknown";
		}
	}
}
