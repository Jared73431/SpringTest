// 1. 抽象基類 - 提供共通功能
@Log4j2
public abstract class BaseBatchScheduler {
    
    protected final JobLauncher jobLauncher;
    protected final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public BaseBatchScheduler(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }
    
    /**
     * 創建標準作業參數
     */
    protected JobParameters createJobParameters(String executionType, String jobName) {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("executionType", executionType)
                .addString("scheduledTime", LocalDateTime.now().format(formatter))
                .addString("jobId", jobName + "_" + System.currentTimeMillis())
                .toJobParameters();
    }
    
    /**
     * 安全執行作業的通用方法
     */
    protected void executeJobSafely(Job job, String executionType, String jobDescription) {
        try {
            JobParameters params = createJobParameters(executionType, job.getName());
            
            log.info("🚀 Starting {} at {}", jobDescription, LocalDateTime.now().format(formatter));
            JobExecution jobExecution = jobLauncher.run(job, params);
            
            log.info("📊 {} Result: {} (ID: {})", 
                    jobDescription, jobExecution.getStatus(), jobExecution.getJobId());
                    
        } catch (JobExecutionException e) {
            log.error("❌ {} execution failed: {}", jobDescription, e.getMessage(), e);
        }
    }
}

// 2. 用戶相關批次排程器
@Configuration
@EnableScheduling
public class UserBatchScheduler extends BaseBatchScheduler {
    
    @Autowired
    @Qualifier("importUserJob4")
    private Job importUserJob4;
    
    @Autowired
    @Qualifier("healthInsuranceJob")
    private Job healthInsuranceJob;
    
    public UserBatchScheduler(JobLauncher jobLauncher) {
        super(jobLauncher);
    }
    
    @Scheduled(fixedRate = 300000) // 5分鐘
    public void runImportUserJob() {
        executeJobSafely(importUserJob4, "USER_IMPORT_SCHEDULED", "User Import Job");
    }
    
    @Scheduled(fixedRate = 120000) // 2分鐘
    public void runHealthInsuranceJob() {
        executeJobSafely(healthInsuranceJob, "HEALTH_INSURANCE_SCHEDULED", "Health Insurance Job");
    }
}

// 3. 書籍相關批次排程器
@Configuration
@EnableScheduling
public class BookBatchScheduler extends BaseBatchScheduler {
    
    @Autowired
    @Qualifier("bookRepositoryJob")
    private Job bookRepositoryJob;
    
    @Autowired
    @Qualifier("bookJpaJob")
    private Job bookJpaJob;
    
    @Autowired
    @Qualifier("bookJdbcJob")
    private Job bookJdbcJob;
    
    public BookBatchScheduler(JobLauncher jobLauncher) {
        super(jobLauncher);
    }
    
    @Scheduled(fixedRate = 600000) // 10分鐘
    public void runBookRepositoryJob() {
        executeJobSafely(bookRepositoryJob, "BOOK_REPOSITORY_SCHEDULED", "Book Repository Job");
    }
    
    @Scheduled(fixedRate = 900000) // 15分鐘
    public void runBookJpaJob() {
        executeJobSafely(bookJpaJob, "BOOK_JPA_SCHEDULED", "Book JPA Job");
    }
    
    @Scheduled(fixedRate = 1200000) // 20分鐘
    public void runBookJdbcJob() {
        executeJobSafely(bookJdbcJob, "BOOK_JDBC_SCHEDULED", "Book JDBC Job");
    }
}

// 4. 系統維護批次排程器
@Configuration
@EnableScheduling
public class MaintenanceBatchScheduler extends BaseBatchScheduler {
    
    @Autowired
    @Qualifier("bookComprehensiveJob")
    private Job bookComprehensiveJob;
    
    public MaintenanceBatchScheduler(JobLauncher jobLauncher) {
        super(jobLauncher);
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2點
    public void runDailyMaintenanceJob() {
        executeJobSafely(bookComprehensiveJob, "DAILY_MAINTENANCE", "Daily Maintenance Job");
    }
    
    @Scheduled(cron = "0 0 1 * * MON") // 每週一凌晨1點
    public void runWeeklyCleanupJob() {
        executeJobSafely(bookComprehensiveJob, "WEEKLY_CLEANUP", "Weekly Cleanup Job");
    }
}

// 5. 批次排程管理器 - 統一管理和監控
@Component
@Log4j2
public class BatchSchedulerManager {
    
    private final UserBatchScheduler userScheduler;
    private final BookBatchScheduler bookScheduler;
    private final MaintenanceBatchScheduler maintenanceScheduler;
    
    public BatchSchedulerManager(UserBatchScheduler userScheduler,
                               BookBatchScheduler bookScheduler,
                               MaintenanceBatchScheduler maintenanceScheduler) {
        this.userScheduler = userScheduler;
        this.bookScheduler = bookScheduler;
        this.maintenanceScheduler = maintenanceScheduler;
    }
    
    /**
     * 手動觸發指定類型的作業
     */
    public String triggerJobManually(String schedulerType, String jobType) {
        try {
            switch (schedulerType.toUpperCase()) {
                case "USER" -> {
                    switch (jobType.toUpperCase()) {
                        case "IMPORT" -> {
                            userScheduler.runImportUserJob();
                            return "✅ User Import Job triggered successfully";
                        }
                        case "INSURANCE" -> {
                            userScheduler.runHealthInsuranceJob();
                            return "✅ Health Insurance Job triggered successfully";
                        }
                        default -> {
                            return "❌ Unknown user job type: " + jobType;
                        }
                    }
                }
                case "BOOK" -> {
                    switch (jobType.toUpperCase()) {
                        case "REPOSITORY" -> {
                            bookScheduler.runBookRepositoryJob();
                            return "✅ Book Repository Job triggered successfully";
                        }
                        case "JPA" -> {
                            bookScheduler.runBookJpaJob();
                            return "✅ Book JPA Job triggered successfully";
                        }
                        case "JDBC" -> {
                            bookScheduler.runBookJdbcJob();
                            return "✅ Book JDBC Job triggered successfully";
                        }
                        default -> {
                            return "❌ Unknown book job type: " + jobType;
                        }
                    }
                }
                case "MAINTENANCE" -> {
                    switch (jobType.toUpperCase()) {
                        case "DAILY" -> {
                            maintenanceScheduler.runDailyMaintenanceJob();
                            return "✅ Daily Maintenance Job triggered successfully";
                        }
                        case "WEEKLY" -> {
                            maintenanceScheduler.runWeeklyCleanupJob();
                            return "✅ Weekly Cleanup Job triggered successfully";
                        }
                        default -> {
                            return "❌ Unknown maintenance job type: " + jobType;
                        }
                    }
                }
                default -> {
                    return "❌ Unknown scheduler type: " + schedulerType;
                }
            }
        } catch (Exception e) {
            log.error("❌ Manual job trigger failed: {}", e.getMessage(), e);
            return "❌ Job execution failed: " + e.getMessage();
        }
    }
    
    /**
     * 獲取所有排程器的狀態
     */
    public Map<String, Object> getSchedulerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        status.put("userScheduler", "Active");
        status.put("bookScheduler", "Active");
        status.put("maintenanceScheduler", "Active");
        return status;
    }
}