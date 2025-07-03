package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.model.Post;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
public class HttpClientTest {

    @Autowired
    private HttpClientService httpClientService;

    @Test
    public void testGetPost() {
        Mono<Post> postMono = httpClientService.getPost(1L);

        StepVerifier.create(postMono)
                .expectNextMatches(post ->
                        post.getId() == 1L &&
                                post.getUserId() == 1L &&
                                post.getTitle() != null &&
                                post.getBody() != null
                )
                .verifyComplete();
    }

    @Test
    public void testCreatePost() {
        Post newPost = new Post(1L, "測試標題", "測試內容");

        Mono<Post> createdPostMono = httpClientService.createPost(newPost);

        StepVerifier.create(createdPostMono)
                .expectNextMatches(post ->
                        post.getUserId() == 1L &&
                                "測試標題".equals(post.getTitle()) &&
                                "測試內容".equals(post.getBody())
                )
                .verifyComplete();
    }

    @Test
    public void testGetAllPosts() {
        StepVerifier.create(httpClientService.getAllPosts())
                .expectNextCount(100) // JSONPlaceholder 有100筆資料
                .verifyComplete();
    }
}
