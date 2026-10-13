package com.example.demo.health;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * 自訂的健康檢查：存放書籍封面的目錄是否存在、可以寫入（第 2 課）。
 *
 * <p>
 * Bean 名稱 coverStorageHealthIndicator 去掉 HealthIndicator，就是 /actuator/health 裡的「coverStorage」。
 * 回報 DOWN 時，整體的 health 也是 DOWN（HTTP 503）。
 *
 * <p>
 * Spring Boot 4 起，HealthIndicator 的 package 是 org.springframework.boot.health.contributor
 * （舊版是 org.springframework.boot.actuate.health）。
 */
@Component
public class CoverStorageHealthIndicator extends AbstractHealthIndicator {

	private final Path directory;

	public CoverStorageHealthIndicator(@Value("${app.cover-storage.dir}") Path directory) {
		this.directory = directory;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		builder.withDetail("directory", directory.toString());
		if (Files.isDirectory(directory) && Files.isWritable(directory)) {
			builder.up();
		}
		else {
			builder.down().withDetail("error", "directory does not exist or is not writable");
		}
	}
}
