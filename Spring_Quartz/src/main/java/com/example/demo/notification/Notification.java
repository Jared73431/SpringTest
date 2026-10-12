package com.example.demo.notification;

import java.time.Instant;

/** 發送紀錄：sentBy 是執行這次 Job 的 Quartz 實例（叢集時可以看出是哪一台） */
public record Notification(String recipient, String message, Instant sentAt, String sentBy) {
}
