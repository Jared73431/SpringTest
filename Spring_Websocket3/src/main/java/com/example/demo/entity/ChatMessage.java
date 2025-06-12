package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessage {


    private MessageType type;
    private String content;
    private String sender;
    private String timestamp;
    private String roomId; // 新增房間ID字段

    public enum MessageType {
        CHAT, JOIN, LEAVE, USER_LIST // 新增 USER_LIST 類型
    }

    // 建構函數
    public ChatMessage() {}

    public ChatMessage(MessageType type, String content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.timestamp = LocalDateTime.now().toString();
    }

    public ChatMessage(MessageType type, String content, String sender, String roomId) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.roomId = roomId;
        this.timestamp = LocalDateTime.now().toString();
    }
}
