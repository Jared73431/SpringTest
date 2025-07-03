package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Post;
import com.example.demo.service.HttpClientService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private HttpClientService httpClientService;

    /**
     * 測試GET請求
     * GET /test/get/{id}
     */
    @GetMapping("/get/{id}")
    public Mono<Post> testGet(@PathVariable Long id) {
        System.out.println("測試GET請求，ID: " + id);
        return httpClientService.getPost(id)
                .doOnNext(post -> System.out.println("GET結果: " + post))
                .doOnError(error -> System.err.println("GET錯誤: " + error.getMessage()));
    }

    /**
     * 測試POST請求 - 使用URL參數
     * POST /test/post-simple?userId=1&title=標題&body=內容
     */
    @PostMapping("/post-simple")
    public Mono<Post> testPostSimple(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "預設標題") String title,
            @RequestParam(defaultValue = "預設內容") String body) {

        System.out.println("測試POST請求（URL參數）");
        System.out.println("userId: " + userId + ", title: " + title + ", body: " + body);

        Post post = new Post(userId, title, body);
        return httpClientService.createPost(post)
                .doOnNext(result -> System.out.println("POST結果: " + result))
                .doOnError(error -> System.err.println("POST錯誤: " + error.getMessage()));
    }

    /**
     * 測試POST請求 - 使用JSON請求體
     * POST /test/post-json
     * Content-Type: application/json
     * Body: {"userId": 1, "title": "標題", "body": "內容"}
     */
    @PostMapping("/post-json")
    public Mono<Post> testPostJson(@RequestBody Post post) {
        System.out.println("測試POST請求（JSON）");
        System.out.println("收到的POST資料: " + post);

        return httpClientService.createPost(post)
                .doOnNext(result -> System.out.println("POST結果: " + result))
                .doOnError(error -> System.err.println("POST錯誤: " + error.getMessage()));
    }

    /**
     * 建立測試資料的便捷方法
     * GET /test/create-sample
     */
    @GetMapping("/create-sample")
    public Mono<Post> createSamplePost() {
        System.out.println("建立範例貼文");
        Post samplePost = new Post(1L, "範例標題", "這是一個範例貼文內容");

        return httpClientService.createPost(samplePost)
                .doOnNext(result -> System.out.println("範例貼文建立結果: " + result))
                .doOnError(error -> System.err.println("建立範例貼文錯誤: " + error.getMessage()));
    }
}
