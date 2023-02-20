package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.QuartzService;
import com.example.demo.task.HelloJob;

@RestController
@RequestMapping("/quartz")
public class QuartzController {

    @Autowired
    private QuartzService quartzService;

    private String jobName = "myJob";
    private String jobGroupName = "myGroup";
    
    @GetMapping(value = "/add")
    public String addJob() throws Exception {
        quartzService.addJob(HelloJob.class, "0/5 * * * * ?");
        return "ok";
    }

    @GetMapping(value = "/remove")
    public String removeJob() throws Exception {
        quartzService.removeJob(jobName, jobGroupName);
        return "ok";
    }

    @GetMapping("/start")
    public String startJob() throws Exception {
        quartzService.startJobs();
        return "ok";
    }

    @GetMapping("/shutdown")
    public String shutdownJob() throws Exception {
        quartzService.shutdownJobs();
        return "ok";
    }
}
