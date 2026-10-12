package com.example.demo.summary;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.example.demo.notification.NotificationService;

/** 每天統計已發送的提醒數量（寫進 log） */
public class DailySummaryJob extends QuartzJobBean {

	private static final Logger log = LoggerFactory.getLogger(DailySummaryJob.class);

	private final NotificationService notificationService;

	public DailySummaryJob(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) {
		log.info("Daily summary: {} notifications sent", notificationService.sent().size());
	}
}
