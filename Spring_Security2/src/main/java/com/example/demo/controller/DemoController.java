package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    
    @GetMapping(value = "/")
    public String demo() {
        return "Spring Boot + Spring Security Configuration Demo";
    }
    
    @GetMapping(value = "/ignore1")
    public String ignore1() {
        return "Spring Boot + Spring Security Configuration ignore1";
    }

}