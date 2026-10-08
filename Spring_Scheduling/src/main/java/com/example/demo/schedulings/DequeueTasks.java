package com.example.demo.schedulings;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DequeueTasks {

	 TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
	 
	@Scheduled(fixedDelay = 60000L, zone = "timeZone")
	public void TaskDequeueFixedDelay() {
		System.out.println("FixeDelay");
	}

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
	public void TaskDequeueFixedRate() {
		System.out.println("fixedRate");
	}

//	@Scheduled(initialDelay =600000)
//	public void TaskDequeueInitialDelay() {
//		System.out.println("initialDelay");
//	}

	@Scheduled(cron = "*/1 * * * * *")
	public void TaskDequeueCron() {
		System.out.println("TaskDequeueCron");
	}
}
