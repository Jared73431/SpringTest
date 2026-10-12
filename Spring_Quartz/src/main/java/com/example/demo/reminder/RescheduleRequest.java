package com.example.demo.reminder;

import jakarta.validation.constraints.NotBlank;

/** 修改提醒的時間（例如門市調整了打烊時間） */
public record RescheduleRequest(@NotBlank String cron, String timeZone) {
}
