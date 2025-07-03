package com.example.demo.service;

import com.example.demo.model.Post;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class HttpClientService {

    private final WebClient webClient;

    public HttpClientService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com")
                .build();
    }

    /**
     * GET 請求 - 獲取單一貼文
     * @param id 貼文ID
     * @return 貼文資料
     */
    public Mono<Post> getPost(Long id) {
        return webClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class);
    }

    /**
     * GET 請求 - 獲取所有貼文
     * @return 所有貼文資料
     */
    public Flux<Post> getAllPosts() {
        return webClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(Post.class);
    }

    /**
     * POST 請求 - 建立新貼文
     * @param post 要建立的貼文資料
     * @return 建立後的貼文資料
     */
    public Mono<Post> createPost(Post post) {
        return webClient.post()
                .uri("/posts")
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class);
    }

    /**
     * PUT 請求 - 更新貼文
     * @param id 貼文ID
     * @param post 更新的貼文資料
     * @return 更新後的貼文資料
     */
    public Mono<Post> updatePost(Long id, Post post) {
        return webClient.put()
                .uri("/posts/{id}", id)
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class);
    }

    /**
     * DELETE 請求 - 刪除貼文
     * @param id 貼文ID
     * @return 空的 Mono
     */
    public Mono<Void> deletePost(Long id) {
        return webClient.delete()
                .uri("/posts/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
