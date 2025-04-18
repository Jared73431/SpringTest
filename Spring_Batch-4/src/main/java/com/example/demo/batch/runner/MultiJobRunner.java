package com.example.demo.batch.runner;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.batch.service.AsyncJobLauncherService;

@Component
public class MultiJobRunner implements CommandLineRunner {

    @Autowired
    private AsyncJobLauncherService asyncJobLauncherService;

    @Autowired
    private Job importUserJob4;

    @Autowired
    private Job sampleJob;

    @Autowired
    private Job healthInsuranceJob;

    @Override
    public void run(String... args) throws Exception {

        JobParameters params1 = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobParameters params2 = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis() + 1000)
                .toJobParameters();

        JobParameters params3 = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis() + 2000)
                .toJobParameters();

        asyncJobLauncherService.launchJob(importUserJob4, params1);
        asyncJobLauncherService.launchJob(sampleJob, params2);
        asyncJobLauncherService.launchJob(healthInsuranceJob, params3);
    }
}
