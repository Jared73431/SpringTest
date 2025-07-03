package com.example.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.model.Post;
import com.example.demo.service.HttpClientService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/posts")
public class HttpController {

    @Autowired
    private HttpClientService httpClientService;

    /**
     * GET /api/posts/{id} - 獲取單一貼文
     */
    @GetMapping("/{id}")
    public Mono<Post> getPost(@PathVariable Long id) {
        return httpClientService.getPost(id);
    }

    /**
     * GET /api/posts - 獲取所有貼文
     */
    @GetMapping
    public Flux<Post> getAllPosts() {
        return httpClientService.getAllPosts();
    }

    /**
     * POST /api/posts - 建立新貼文
     */
    @PostMapping
    public Mono<Post> createPost(@RequestBody Post post) {
        // 驗證請求體是否為空
        if (post == null) {
            return Mono.error(new IllegalArgumentException("請求體不能為空"));
        }
        return httpClientService.createPost(post);
    }

    /**
     * PUT /api/posts/{id} - 更新貼文
     */
    @PutMapping("/{id}")
    public Mono<Post> updatePost(@PathVariable Long id, @RequestBody Post post) {
        return httpClientService.updatePost(id, post);
    }

    /**
     * DELETE /api/posts/{id} - 刪除貼文
     */
    @DeleteMapping("/{id}")
    public Mono<Void> deletePost(@PathVariable Long id) {
        return httpClientService.deletePost(id);
    }
}
