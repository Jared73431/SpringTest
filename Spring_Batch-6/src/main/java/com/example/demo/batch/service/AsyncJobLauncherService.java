package com.example.demo.batch.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncJobLauncherService {

    @Autowired
    private JobLauncher jobLauncher;

    @Async
    public void launchJob(Job job, JobParameters params) {
        try {
            jobLauncher.run(job, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
