package com.example.demo.batch.listener;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import com.example.demo.batch.repository.BookRepository;

import lombok.extern.log4j.Log4j2;

/**
 * 書籍批次作業完成監聽器
 * 功能：
 * 1. 監控作業執行狀態
 * 2. 記錄執行統計信息
 * 3. 生成執行報告
 * 4. 處理異常情況
 */
@Log4j2
@Component
public class BookJobCompletionListener implements JobExecutionListener {

    private final BookRepository bookRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BookJobCompletionListener(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * 作業開始前的回調
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=".repeat(60));
        log.info("📚 BOOK BATCH JOB STARTED");
        log.info("Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job ID: {}", jobExecution.getJobInstance().getId());
        log.info("Start Time: {}", jobExecution.getStartTime().format(formatter));
        log.info("Parameters: {}", jobExecution.getJobParameters());

        // 記錄資料庫狀態
        long totalBooks = bookRepository.count();
        log.info("📊 Database Status: {} books available for processing", totalBooks);
        log.info("=".repeat(60));
    }

    /**
     * 作業完成後的回調
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        BatchStatus status = jobExecution.getStatus();

        log.info("=".repeat(60));
        log.info("📚 BOOK BATCH JOB COMPLETED");
        log.info("Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job Status: {}", status);
        log.info("Start Time: {}", jobExecution.getStartTime().format(formatter));
        log.info("End Time: {}", jobExecution.getEndTime().format(formatter));

        // 計算執行時間
        Duration duration = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
        log.info("⏱️ Execution Time: {} minutes {} seconds",
                duration.toMinutes(), duration.getSeconds() % 60);

        // 處理不同的執行狀態
        switch (status) {
            case COMPLETED -> handleCompletedJob(jobExecution);
            case FAILED -> handleFailedJob(jobExecution);
            case STOPPED -> handleStoppedJob(jobExecution);
            default -> log.info("Job finished with status: {}", status);
        }

        log.info("=".repeat(60));
    }

    /**
     * 處理成功完成的作業
     */
    private void handleCompletedJob(JobExecution jobExecution) {
        log.info("✅ JOB COMPLETED SUCCESSFULLY!");

        // 統計所有步驟的執行信息
        int totalSteps = jobExecution.getStepExecutions().size();
        long totalItemsRead = 0;
        long totalItemsWritten = 0;
        long totalItemsProcessed = 0;
        long totalSkipped = 0;

        log.info("📈 STEP EXECUTION SUMMARY:");
        log.info("Total Steps: {}", totalSteps);

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            String stepName = stepExecution.getStepName();
            long readCount = stepExecution.getReadCount();
            long writeCount = stepExecution.getWriteCount();
            long processCount = stepExecution.getProcessSkipCount();
            long skipCount = stepExecution.getSkipCount();

            totalItemsRead += readCount;
            totalItemsWritten += writeCount;
            totalItemsProcessed += (readCount - skipCount);
            totalSkipped += skipCount;

            log.info("  📋 Step: {} | Read: {} | Written: {} | Skipped: {}",
                    stepName, readCount, writeCount, skipCount);

            // 顯示步驟執行時間
            if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
                Duration stepDuration = Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime());
                log.info("      ⏱️ Step Duration: {}ms", stepDuration.toMillis());
            }
        }

        // 總體統計
        log.info("📊 OVERALL STATISTICS:");
        log.info("  Total Items Read: {}", totalItemsRead);
        log.info("  Total Items Processed: {}", totalItemsProcessed);
        log.info("  Total Items Written: {}", totalItemsWritten);
        log.info("  Total Items Skipped: {}", totalSkipped);

        // 計算處理效率
        if (totalItemsRead > 0) {
            double successRate = ((double) totalItemsProcessed / totalItemsRead) * 100;
            log.info("  📈 Processing Success Rate: {:.2f}%", successRate);
        }

        // 驗證資料庫最終狀態
        verifyDatabaseState();
    }

    /**
     * 處理失敗的作業
     */
    private void handleFailedJob(JobExecution jobExecution) {
        log.error("❌ JOB FAILED!");

        // 記錄失敗原因
        jobExecution.getAllFailureExceptions().forEach(exception -> {
            log.error("💥 Failure Exception: {}", exception.getMessage(), exception);
        });

        // 分析失敗的步驟
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStatus() == BatchStatus.FAILED) {
                log.error("❌ Failed Step: {}", stepExecution.getStepName());
                log.error("   Error: {}", stepExecution.getExitStatus().getExitDescription());

                stepExecution.getFailureExceptions().forEach(exception -> {
                    log.error("   Exception: {}", exception.getMessage(), exception);
                });
            }
        }

        // 提供恢復建議
        log.info("🔧 Recovery Suggestions:");
        log.info("  1. Check database connectivity");
        log.info("  2. Verify input data format");
        log.info("  3. Review processing logic");
        log.info("  4. Check available memory and resources");
    }

    /**
     * 處理停止的作業
     */
    private void handleStoppedJob(JobExecution jobExecution) {
        log.warn("⏹️ JOB WAS STOPPED!");
        log.info("Job was manually stopped or interrupted");

        // 記錄已完成的工作
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                log.info("✅ Completed Step: {} (Read: {}, Written: {})",
                        stepExecution.getStepName(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount());
            }
        }
    }

    /**
     * 驗證資料庫狀態
     */
    private void verifyDatabaseState() {
        try {
            long totalBooks = bookRepository.count();
            log.info("📚 Final Database State: {} total books", totalBooks);

            // 可以添加更多驗證邏輯
            if (totalBooks > 0) {
                log.info("✅ Database verification successful");
            } else {
                log.warn("⚠️ No books found in database after processing");
            }

        } catch (Exception e) {
            log.error("❌ Database verification failed: {}", e.getMessage(), e);
        }
    }
}
