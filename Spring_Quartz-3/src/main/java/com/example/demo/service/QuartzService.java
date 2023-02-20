package com.example.demo.service;

public interface QuartzService {
    void addJob(Class jobClass, String time);

    void addJob(Class jobClass, String time, String jobName, String jobGroupName, String triggerName, String triggerGroupName);

    void removeJob(String jobName, String jobGroupName);

    void startJobs();

    void shutdownJobs();
}
