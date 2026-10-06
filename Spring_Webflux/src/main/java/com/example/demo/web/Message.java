package com.example.demo.web;

import java.time.Instant;

public record Message(long id, String text, Instant time) {
}
