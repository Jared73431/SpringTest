package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PrivateMessage {

    private String sender;
    private String recipient;
    private String content;
    private String timestamp;

    public PrivateMessage() {}

    public PrivateMessage(String sender, String recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = LocalDateTime.now().toString();
    }
}
