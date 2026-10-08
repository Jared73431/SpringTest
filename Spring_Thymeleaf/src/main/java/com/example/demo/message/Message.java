package com.example.demo.message;

import java.time.LocalDateTime;

public record Message(long id, String author, String content, LocalDateTime createdAt) {
}
