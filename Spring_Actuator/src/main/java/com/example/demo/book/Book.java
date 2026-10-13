package com.example.demo.book;

import java.time.Instant;

public record Book(Long id, String title, String author, Instant createdAt) {
}
