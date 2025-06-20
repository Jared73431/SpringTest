package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final MessageRepository messageRepository;

    /**
     * 定期清理舊消息（保留30天）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2點執行
    @Transactional
    public void cleanupOldMessages() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            messageRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("清理30天前的舊消息完成");
        } catch (Exception e) {
            log.error("清理舊消息失敗: {}", e.getMessage());
        }
    }
}
