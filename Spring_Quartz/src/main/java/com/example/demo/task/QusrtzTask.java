package com.example.demo.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class QusrtzTask {

    public void sayHello() {
        SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss");
        System.out.println("Hello:" + sf.format(new Date()));
    }
}
