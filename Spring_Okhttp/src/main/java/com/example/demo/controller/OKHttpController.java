package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.OKHttpService;

@RestController
@RequestMapping("/api/okhttp")
public class OKHttpController {

    @Autowired
    private OKHttpService okHttpService;

    @GetMapping("/test-get")
    public ResponseEntity<String> testGet() {
        try {
            okHttpService.performGetRequest();
            return ResponseEntity.ok("GET 請求執行完成，請查看控制台日誌");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("GET 請求執行失敗: " + e.getMessage());
        }
    }

    @PostMapping("/test-post")
    public ResponseEntity<String> testPost() {
        try {
            okHttpService.performPostRequest();
            return ResponseEntity.ok("POST 請求執行完成，請查看控制台日誌");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("POST 請求執行失敗: " + e.getMessage());
        }
    }

    @GetMapping("/test-async")
    public ResponseEntity<String> testAsync() {
        try {
            okHttpService.performAsyncRequest();
            return ResponseEntity.ok("異步請求已啟動，請查看控制台日誌");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("異步請求啟動失敗: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OKHttp Service is running!");
    }
}
