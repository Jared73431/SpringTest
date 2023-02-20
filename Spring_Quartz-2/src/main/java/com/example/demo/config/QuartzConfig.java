package com.example.demo.config;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.example.demo.task.QuartzJob1;

@Configuration
public class QuartzConfig {

	@Bean
	public JobDetail jobDetail1() {
		return JobBuilder.newJob(QuartzJob1.class).storeDurably().build();
//		return JobBuilder.newJob(QuartzJob1.class).withIdentity("job1","group1").build();
	}

	@Bean
	public Trigger trigger1() {
		SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder
				.simpleSchedule()
				.withIntervalInSeconds(1) // 每一秒執行一次
				.repeatForever(); // 永久重複，一直執行下去
		return TriggerBuilder.newTrigger()
				.forJob(jobDetail1())
				.withSchedule(scheduleBuilder)
				.build();
	}
	
	@Bean
	public JobDetail jobDetail2(){
	    QuartzJobBean quartzJob2 = new QuartzJobBean() {
	        @Override
	        protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
	            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	            System.out.println("內部類quartzJob2----" + sdf.format(new Date()));
	        }
	    };
	    return JobBuilder.newJob(quartzJob2.getClass()).storeDurably().build();
	}

	@Bean
	public Trigger trigger2(){
	    //JobDetail的bean注入不能省略
	    //JobDetail jobDetail3 = JobBuilder.newJob(QuartzJob2.class).storeDurably().build();
	    SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
	            .withIntervalInSeconds(2) //每2秒執行一次
	            .repeatForever(); //永久重複，一直執行下去
	    return TriggerBuilder.newTrigger()
	            .forJob(jobDetail2())
	            .withSchedule(scheduleBuilder).build();
	}
}
