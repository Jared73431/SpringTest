package com.example.demo.reminder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增提醒。cron 使用 Quartz 的格式（秒 分 時 日 月 星期，日與星期其中一個要寫 ?），例如每天 22:30 是 0 30 22 * * ?
 */
public record ReminderRequest(
		@NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9-]+", message = "只能使用小寫英文、數字與 -") String name,
		@NotBlank String cron,
		String timeZone,
		@NotBlank @Size(max = 100) String recipient,
		@NotBlank @Size(max = 500) String message) {
}
