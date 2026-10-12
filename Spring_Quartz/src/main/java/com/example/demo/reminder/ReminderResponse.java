package com.example.demo.reminder;

import java.time.Instant;

/** state：NORMAL（正常）、PAUSED（暫停）… 由 Quartz 的 TriggerState 而來 */
public record ReminderResponse(
		String name,
		String cron,
		String timeZone,
		String recipient,
		String message,
		String state,
		Instant nextFireTime,
		Instant previousFireTime) {
}
