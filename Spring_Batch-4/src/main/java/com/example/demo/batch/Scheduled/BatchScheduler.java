package com.example.demo.batch.Scheduled;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final List<Job> jobs;

    public BatchScheduler(JobLauncher jobLauncher, List<Job> jobs) {
        this.jobLauncher = jobLauncher;
        this.jobs = jobs;
    }

    // 每隔5分鐘執行一次
    @Scheduled(fixedRate = 300000)
    public void perform() throws JobExecutionException {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis()) // 使用時間戳以保證每次執行都有不同的參數
                .toJobParameters();
        for (Job job : jobs) {
            System.out.println("Starting job: " + job.getName());
            JobExecution jobExecution = jobLauncher.run(job, params);
            System.out.println("Batch job " + job.getName() + " status: " + jobExecution.getStatus());
        }
    }
}
