package com.example.demo.batch.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.batch.Scheduled.BookBatchScheduler;
import com.example.demo.batch.repository.BookRepository;

import lombok.extern.log4j.Log4j2;

/**
 * 書籍批次作業 REST API 控制器
 * 提供手動觸發批次作業的 HTTP 端點
 */
@Log4j2
@RestController
@RequestMapping("/api/batch/books")
public class BookBatchController {

    private final JobLauncher jobLauncher;
    private final BookRepository bookRepository;
    private final BookBatchScheduler bookBatchScheduler;
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

    public BookBatchController(JobLauncher jobLauncher,
                               BookRepository bookRepository,
                               BookBatchScheduler bookBatchScheduler) {
        this.jobLauncher = jobLauncher;
        this.bookRepository = bookRepository;
        this.bookBatchScheduler = bookBatchScheduler;
    }

    /**
     * 觸發 Repository 讀取器作業
     * GET /api/batch/books/repository
     */
    @GetMapping("/repository")
    public ResponseEntity<Map<String, Object>> triggerRepositoryJob() {
        return executeJob(bookRepositoryJob, "REPOSITORY", "Repository Item Reader");
    }

    /**
     * 觸發 JPA 分頁讀取器作業
     * GET /api/batch/books/jpa
     */
    @GetMapping("/jpa")
    public ResponseEntity<Map<String, Object>> triggerJpaJob() {
        return executeJob(bookJpaJob, "JPA", "JPA Paging Item Reader");
    }

    /**
     * 觸發 JDBC 游標讀取器作業
     * GET /api/batch/books/jdbc
     */
    @GetMapping("/jdbc")
    public ResponseEntity<Map<String, Object>> triggerJdbcJob() {
        return executeJob(bookJdbcJob, "JDBC", "JDBC Cursor Item Reader");
    }

    /**
     * 觸發綜合作業（包含多個步驟）
     * GET /api/batch/books/comprehensive
     */
    @GetMapping("/comprehensive")
    public ResponseEntity<Map<String, Object>> triggerComprehensiveJob() {
        return executeJob(bookComprehensiveJob, "COMPREHENSIVE", "Comprehensive Multi-Step Job");
    }

    /**
     * 觸發條件作業
     * GET /api/batch/books/conditional
     */
    @GetMapping("/conditional")
    public ResponseEntity<Map<String, Object>> triggerConditionalJob() {
        return executeJob(bookConditionalJob, "CONDITIONAL", "Conditional Flow Job");
    }

    /**
     * 使用排程器手動觸發作業
     * POST /api/batch/books/trigger/{jobType}
     */
    @PostMapping("/trigger/{jobType}")
    public ResponseEntity<Map<String, Object>> triggerJobViaScheduler(@PathVariable String jobType) {
        try {
            String result = bookBatchScheduler.triggerJobManually(jobType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.startsWith("✅"));
            response.put("message", result);
            response.put("jobType", jobType.toUpperCase());
            response.put("triggerTime", LocalDateTime.now().format(formatter));
            response.put("method", "SCHEDULER");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error triggering job via scheduler: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger job: " + e.getMessage());
            errorResponse.put("jobType", jobType.toUpperCase());
            errorResponse.put("triggerTime", LocalDateTime.now().format(formatter));
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 獲取所有可用的作業類型
     * GET /api/batch/books/jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getAvailableJobs() {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> jobs = new HashMap<>();
        jobs.put("REPOSITORY", "Repository Item Reader Job");
        jobs.put("JPA", "JPA Paging Item Reader Job");
        jobs.put("JDBC", "JDBC Cursor Item Reader Job");
        jobs.put("COMPREHENSIVE", "Comprehensive Multi-Step Job");
        jobs.put("CONDITIONAL", "Conditional Flow Job");

        response.put("availableJobs", jobs);
        response.put("totalJobs", jobs.size());
        response.put("queryTime", LocalDateTime.now().format(formatter));

        return ResponseEntity.ok(response);
    }

    /**
     * 獲取書籍統計信息
     * GET /api/batch/books/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBookStats() {
        try {
            long totalBooks = bookRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBooks", totalBooks);
            stats.put("queryTime", LocalDateTime.now().format(formatter));

            // 如果有分類統計方法，可以添加更多統計信息
            // stats.put("categoryCounts", bookRepository.countByCategory());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting book stats: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get book statistics: " + e.getMessage());
            errorResponse.put("queryTime", LocalDateTime.now().format(formatter));

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 批量觸發多個作業
     * POST /api/batch/books/batch-trigger
     */
    @PostMapping("/batch-trigger")
    public ResponseEntity<Map<String, Object>> triggerMultipleJobs(@RequestBody List<String> jobTypes) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> results = new HashMap<>();

        for (String jobType : jobTypes) {
            try {
                String result = bookBatchScheduler.triggerJobManually(jobType);
                results.put(jobType, Map.of(
                        "success", result.startsWith("✅"),
                        "message", result
                ));
            } catch (Exception e) {
                results.put(jobType, Map.of(
                        "success", false,
                        "message", "Failed: " + e.getMessage()
                ));
            }
        }

        response.put("results", results);
        response.put("totalJobs", jobTypes.size());
        response.put("triggerTime", LocalDateTime.now().format(formatter));

        return ResponseEntity.ok(response);
    }

    /**
     * 健康檢查端點
     * GET /api/batch/books/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 檢查數據庫連接
            long bookCount = bookRepository.count();

            health.put("status", "UP");
            health.put("database", "Connected");
            health.put("bookCount", bookCount);
            health.put("checkTime", LocalDateTime.now().format(formatter));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("database", "Disconnected");
            health.put("error", e.getMessage());
            health.put("checkTime", LocalDateTime.now().format(formatter));

            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * 通用作業執行方法
     */
    private ResponseEntity<Map<String, Object>> executeJob(Job job, String jobType, String description) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("executionType", "MANUAL_API")
                    .addString("jobType", jobType)
                    .addString("triggerTime", LocalDateTime.now().format(formatter))
                    .toJobParameters();

            log.info("🚀 Starting {} job via API", jobType);
            JobExecution jobExecution = jobLauncher.run(job, params);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", job.getName());
            response.put("jobType", jobType);
            response.put("description", description);
            response.put("executionId", jobExecution.getId());
            response.put("status", jobExecution.getStatus().toString());
            response.put("startTime", jobExecution.getStartTime());
            response.put("triggerTime", LocalDateTime.now().format(formatter));
            response.put("method", "DIRECT_API");

            log.info("✅ {} job completed with status: {}", jobType, jobExecution.getStatus());

            return ResponseEntity.ok(response);

        } catch (JobExecutionException e) {
            log.error("❌ {} job execution failed: {}", jobType, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("jobName", job.getName());
            errorResponse.put("jobType", jobType);
            errorResponse.put("description", description);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            errorResponse.put("triggerTime", LocalDateTime.now().format(formatter));

            return ResponseEntity.internalServerError().body(errorResponse);
        } catch (Exception e) {
            log.error("❌ Unexpected error in {} job: {}", jobType, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("jobType", jobType);
            errorResponse.put("error", "Unexpected error: " + e.getMessage());
            errorResponse.put("triggerTime", LocalDateTime.now().format(formatter));

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
