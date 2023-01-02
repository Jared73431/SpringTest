package com.example.demo.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

@Component
@FeignClient(name = "service-provider")
public interface BookFeignClient {

    @GetMapping("/findall")
    public List<?> findall();

    @GetMapping("/hello")
	public String hello();
}
