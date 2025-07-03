package com.example.demo.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.model.Post;
import com.example.demo.service.HttpClientService;

@Component
public class DemoRunner implements CommandLineRunner {

    @Autowired
    private HttpClientService httpClientService;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("=== HTTP Client 示範 ===");

        // 1. GET 請求 - 獲取單一貼文
        System.out.println("\n1. GET 請求 - 獲取貼文 ID=1:");
        httpClientService.getPost(1L)
                .doOnNext(post -> System.out.println("結果: " + post))
                .block();

        // 2. POST 請求 - 建立新貼文
        System.out.println("\n2. POST 請求 - 建立新貼文:");
        Post newPost = new Post(1L, "我的新貼文", "這是一個測試貼文內容");
        httpClientService.createPost(newPost)
                .doOnNext(post -> System.out.println("建立結果: " + post))
                .block();

        // 3. GET 請求 - 獲取前5筆貼文
        System.out.println("\n3. GET 請求 - 獲取前5筆貼文:");
        httpClientService.getAllPosts()
                .take(5)
                .doOnNext(post -> System.out.println("貼文: " + post.getTitle()))
                .blockLast();

        // 4. PUT 請求 - 更新貼文
        System.out.println("\n4. PUT 請求 - 更新貼文 ID=1:");
        Post updatePost = new Post(1L, "更新後的標題", "更新後的內容");
        httpClientService.updatePost(1L, updatePost)
                .doOnNext(post -> System.out.println("更新結果: " + post))
                .block();

        // 5. DELETE 請求 - 刪除貼文
        System.out.println("\n5. DELETE 請求 - 刪除貼文 ID=1:");
        httpClientService.deletePost(1L)
                .doOnSuccess(result -> System.out.println("刪除成功"))
                .block();

        System.out.println("\n=== 示範完成 ===");

    }
}
