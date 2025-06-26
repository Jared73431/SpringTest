package com.example.demo.batch.Scheduled;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class BatchScheduler {

    private final JobLauncher jobLauncher;

    @Autowired
    @Qualifier("importUserJob4")
    private Job importUserJob4;

    @Autowired
    @Qualifier("healthInsuranceJob")
    private Job healthInsuranceJob;

    @Autowired
    @Qualifier("sampleJob")
    private Job sampleJob;

    public BatchScheduler(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }

    // importUserJob4 每5分钟执行一次
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void runImportUserJob() throws JobExecutionException {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        System.out.println("Starting job: " + importUserJob4.getName());
        JobExecution jobExecution = jobLauncher.run(importUserJob4, params);
        System.out.println("Batch job " + importUserJob4.getName() + " status: " + jobExecution.getStatus());
    }

    // healthInsuranceJob 每2分钟执行一次
    @Scheduled(fixedRate = 120000) // 2分钟 = 120000毫秒
    public void runHealthInsuranceJob() throws JobExecutionException {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        System.out.println("Starting job: " + healthInsuranceJob.getName());
        JobExecution jobExecution = jobLauncher.run(healthInsuranceJob, params);
        System.out.println("Batch job " + healthInsuranceJob.getName() + " status: " + jobExecution.getStatus());
    }

    // 如果您也需要为sampleJob设置不同的执行周期，可以添加类似的方法
    @Scheduled(fixedRate = 60000) // 1分钟 = 60000毫秒，根据实际需求调整
    public void runSampleJob() throws JobExecutionException {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        System.out.println("Starting job: " + sampleJob.getName());
        JobExecution jobExecution = jobLauncher.run(sampleJob, params);
        System.out.println("Batch job " + sampleJob.getName() + " status: " + jobExecution.getStatus());
    }
}
