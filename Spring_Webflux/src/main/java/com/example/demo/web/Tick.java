package com.example.demo.web;

import java.time.Instant;

public record Tick(long sequence, Instant time) {
}
