package com.example.demo.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class HelloJob implements Job{

//	@Autowired
//	private TimeUtil timeUtil;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println("Hello world!"+sdf.format(new Date())+"取得");
		
	}
	
}
