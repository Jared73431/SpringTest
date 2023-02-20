package com.example.demo.service.impl;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.service.QuartzService;

@Service
public class QuartzServiceImpl implements QuartzService {

    private String jobName = "myJob";
    private String jobGroupName = "myGroup";
    private String triggerName = "myTrigger";
    private String triggerGroupName = "myTriggerGroup";

    /**
     * 注入调度器
     */
    @Autowired
    private Scheduler scheduler;

    /**
     * @param jobClass:任务
     * @param time:时间设置，CronExpression表达式
     * @Description: 添加一个定时任务，使用默认的任务组名，触发器名，触发器组名
     */
    @Override
    public void addJob(Class jobClass, String time) {
        addJob(jobClass, time, jobName, jobGroupName, triggerName, triggerGroupName);
    }

    @Override
    public void addJob(Class jobClass, String time,
                       String jobName, String jobGroupName, String triggerName, String triggerGroupName) {


        JobDetail job = newJob(jobClass)
                .withIdentity(jobName, jobGroupName)
                .build();
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(time);

        CronTrigger trigger = newTrigger().withIdentity(triggerName, triggerGroupName)
                .withSchedule(scheduleBuilder).build();
        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param jobName jobName
     * @Description: 移除一个任务，以及任务的所有trigger
     */
    @Override
    public void removeJob(String jobName, String jobGroupName) {
        try {
            scheduler.deleteJob(jobKey(jobName, jobGroupName));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description:启动所有定时任务
     */
    @Override
    public void startJobs() {
        try {
            scheduler.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @Description:关闭所有定时任务
     */
    @Override
    public void shutdownJobs() {
        try {
            if (!scheduler.isShutdown()) {
                //未传参或false：不等待执行完成便结束；true：等待任务执行完才结束
                scheduler.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
