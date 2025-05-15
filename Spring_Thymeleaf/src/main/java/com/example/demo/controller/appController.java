package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class appController {
    @GetMapping("/hello")
    public String hello() {
        return "hello"; // 要導入的html
    }
}
