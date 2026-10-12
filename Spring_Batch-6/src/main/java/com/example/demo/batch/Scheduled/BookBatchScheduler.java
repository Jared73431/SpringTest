package com.example.demo.batch.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.log4j.Log4j2;

/**
 * 書籍批次作業排程器
 * 提供不同時間間隔的排程任務，演示三種不同的讀取器方式
 */
@Log4j2
@Configuration
@EnableScheduling
public class BookBatchScheduler {

    private final JobLauncher jobLauncher;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    @Qualifier("bookRepositoryJob")
    private Job bookRepositoryJob;

    @Autowired
    @Qualifier("bookJpaJob")
    private Job bookJpaJob;

    @Autowired
    @Qualifier("bookJdbcJob")
    private Job bookJdbcJob;

    @Autowired
    @Qualifier("bookComprehensiveJob")
    private Job bookComprehensiveJob;

    @Autowired
    @Qualifier("bookConditionalJob")
    private Job bookConditionalJob;

    public BookBatchScheduler(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }

    /**
     * Repository 讀取器作業 - 每 10 分鐘執行一次
     * 適合：常規數據同步，簡單查詢場景
     */
    @Scheduled(fixedRate = 600000) // 10分鐘 = 600000毫秒
    public void runBookRepositoryJob() {
        try {
            JobParameters params = createJobParameters("REPOSITORY_SCHEDULED");

            log.info("🚀 Starting Repository Job at {}", LocalDateTime.now().format(formatter));
            JobExecution jobExecution = jobLauncher.run(bookRepositoryJob, params);

            log.info("📊 Repository Job Result: {} (ID: {})",
                    jobExecution.getStatus(), jobExecution.getJobInstance().getId());

        } catch (JobExecutionException e) {
            log.error("❌ Repository Job execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * JPA 分頁讀取器作業 - 每 15 分鐘執行一次
     * 適合：中等複雜度查詢，需要參數化查詢的場景
     */
    @Scheduled(fixedRate = 900000) // 15分鐘 = 900000毫秒
    public void runBookJpaJob() {
        try {
            JobParameters params = createJobParameters("JPA_SCHEDULED");

            log.info("🚀 Starting JPA Job at {}", LocalDateTime.now().format(formatter));
            JobExecution jobExecution = jobLauncher.run(bookJpaJob, params);

            log.info("📊 JPA Job Result: {} (ID: {})",
                    jobExecution.getStatus(), jobExecution.getJobInstance().getId());

        } catch (JobExecutionException e) {
            log.error("❌ JPA Job execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * JDBC 游標讀取器作業 - 每 20 分鐘執行一次
     * 適合：大數據量處理，性能要求高的場景
     */
    @Scheduled(fixedRate = 1200000) // 20分鐘 = 1200000毫秒
    public void runBookJdbcJob() {
        try {
            JobParameters params = createJobParameters("JDBC_SCHEDULED");

            log.info("🚀 Starting JDBC Job at {}", LocalDateTime.now().format(formatter));
            JobExecution jobExecution = jobLauncher.run(bookJdbcJob, params);

            log.info("📊 JDBC Job Result: {} (ID: {})",
                    jobExecution.getStatus(), jobExecution.getJobInstance().getId());

        } catch (JobExecutionException e) {
            log.error("❌ JDBC Job execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 綜合作業 - 每小時執行一次
     * 包含多個步驟的完整數據處理流程
     */
    @Scheduled(fixedRate = 3600000) // 1小時 = 3600000毫秒
    public void runBookComprehensiveJob() {
        try {
            JobParameters params = createJobParameters("COMPREHENSIVE_SCHEDULED");

            log.info("🚀 Starting Comprehensive Job at {}", LocalDateTime.now().format(formatter));
            JobExecution jobExecution = jobLauncher.run(bookComprehensiveJob, params);

            log.info("📊 Comprehensive Job Result: {} (ID: {})",
                    jobExecution.getStatus(), jobExecution.getJobInstance().getId());

        } catch (JobExecutionException e) {
            log.error("❌ Comprehensive Job execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 條件作業 - 每 30 分鐘執行一次
     * 根據條件執行不同的處理邏輯
     */
    @Scheduled(fixedRate = 1800000) // 30分鐘 = 1800000毫秒
    public void runBookConditionalJob() {
        try {
            JobParameters params = createJobParameters("CONDITIONAL_SCHEDULED");

            log.info("🚀 Starting Conditional Job at {}", LocalDateTime.now().format(formatter));
            JobExecution jobExecution = jobLauncher.run(bookConditionalJob, params);

            log.info("📊 Conditional Job Result: {} (ID: {})",
                    jobExecution.getStatus(), jobExecution.getJobInstance().getId());

        } catch (JobExecutionException e) {
            log.error("❌ Conditional Job execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 每日完整報告作業 - 每天凌晨 2 點執行
     * 生成完整的數據處理報告
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2:00
    public void runDailyReportJob() {
        try {
            JobParameters params = createJobParameters("DAILY_REPORT");

            log.info("🌙 Starting Daily Report Job at {}", LocalDateTime.now().format(formatter));

            // 順序執行所有作業以生成完整報告
            log.info("📋 Executing Repository Job...");
            jobLauncher.run(bookRepositoryJob, params);

            Thread.sleep(30000); // 等待 30 秒

            log.info("📋 Executing JPA Job...");
            jobLauncher.run(bookJpaJob, createJobParameters("DAILY_REPORT_JPA"));

            Thread.sleep(30000); // 等待 30 秒

            log.info("📋 Executing JDBC Job...");
            JobExecution finalJob = jobLauncher.run(bookJdbcJob, createJobParameters("DAILY_REPORT_JDBC"));

            log.info("🌅 Daily Report Completed: {}", finalJob.getStatus());

        } catch (Exception e) {
            log.error("❌ Daily Report Job execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 每週數據清理作業 - 每週一凌晨 1 點執行
     * 執行數據維護和清理任務
     */
    @Scheduled(cron = "0 0 1 * * MON") // 每週一凌晨 1:00
    public void runWeeklyMaintenanceJob() {
        try {
            JobParameters params = createJobParameters("WEEKLY_MAINTENANCE");

            log.info("🧹 Starting Weekly Maintenance Job at {}", LocalDateTime.now().format(formatter));
            JobExecution jobExecution = jobLauncher.run(bookComprehensiveJob, params);

            log.info("🧹 Weekly Maintenance Result: {} (ID: {})",
                    jobExecution.getStatus(), jobExecution.getJobInstance().getId());

        } catch (JobExecutionException e) {
            log.error("❌ Weekly Maintenance Job execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 創建作業參數
     */
    private JobParameters createJobParameters(String executionType) {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("executionType", executionType)
                .addString("scheduledTime", LocalDateTime.now().format(formatter))
                .addString("jobId", "BOOK_BATCH_" + System.currentTimeMillis())
                .toJobParameters();
    }

    /**
     * 手動觸發作業的方法 - 可通過 REST API 調用
     */
    public String triggerJobManually(String jobType) {
        try {
            JobParameters params = createJobParameters("MANUAL_TRIGGER");
            Job jobToRun;

            switch (jobType.toUpperCase()) {
                case "REPOSITORY" -> jobToRun = bookRepositoryJob;
                case "JPA" -> jobToRun = bookJpaJob;
                case "JDBC" -> jobToRun = bookJdbcJob;
                case "COMPREHENSIVE" -> jobToRun = bookComprehensiveJob;
                case "CONDITIONAL" -> jobToRun = bookConditionalJob;
                default -> {
                    return "❌ Unknown job type: " + jobType;
                }
            }

            log.info("🎯 Manually triggering {} job", jobType);
            JobExecution jobExecution = jobLauncher.run(jobToRun, params);

            return String.format("✅ Job %s triggered successfully. Status: %s, ID: %d",
                    jobType, jobExecution.getStatus(), jobExecution.getJobInstance().getId());

        } catch (JobExecutionException e) {
            log.error("❌ Manual job trigger failed: {}", e.getMessage(), e);
            return "❌ Job execution failed: " + e.getMessage();
        }
    }
}
