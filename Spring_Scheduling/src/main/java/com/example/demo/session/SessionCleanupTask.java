package com.example.demo.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * 排程「什麼時候做」。
 *
 * <ul>
 * <li>cron 與時區寫在設定檔（app.cleanup.cron / app.cleanup.zone）：不同環境可以有不同的時間，設成 - 就停用</li>
 * <li>zone 只對 cron 有效（修正前把 zone 寫在 fixedDelay 上，而且寫成字串 "timeZone"，設定被默默忽略）</li>
 * <li>\@SchedulerLock：部署多台主機時只有一台會執行；lockAtLeastFor 讓鎖至少保留 30 秒，避免主機之間的時間差造成重複執行</li>
 * </ul>
 */
@Component
public class SessionCleanupTask {

	private static final Logger log = LoggerFactory.getLogger(SessionCleanupTask.class);

	private final SessionCleanupService cleanupService;

	public SessionCleanupTask(SessionCleanupService cleanupService) {
		this.cleanupService = cleanupService;
	}

	@Scheduled(cron = "${app.cleanup.cron}", zone = "${app.cleanup.zone}")
	@SchedulerLock(name = "session-cleanup", lockAtLeastFor = "PT30S")
	public void cleanupExpiredSessions() {
		int deleted = cleanupService.deleteExpired();
		log.info("Deleted {} expired sessions", deleted);
	}
}
