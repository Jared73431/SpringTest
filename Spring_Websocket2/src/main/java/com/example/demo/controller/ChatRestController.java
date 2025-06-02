package com.example.demo.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.ChatMessage;
import com.example.demo.service.ChatMessageService;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    @Autowired
    private ChatMessageService chatMessageService;

    // 獲取最近訊息
    @GetMapping("/recent/{roomId}")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(@PathVariable String roomId) {
        List<ChatMessage> messages = chatMessageService.getRecentMessages(roomId);
        return ResponseEntity.ok(messages);
    }

    // 獲取歷史訊息（分頁）
    @GetMapping("/history/{roomId}")
    public ResponseEntity<Page<ChatMessage>> getMessageHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ChatMessage> messages = chatMessageService.getMessageHistory(roomId, page, size);
        return ResponseEntity.ok(messages);
    }

    // 搜尋訊息
    @GetMapping("/search/{roomId}")
    public ResponseEntity<List<ChatMessage>> searchMessages(
            @PathVariable String roomId,
            @RequestParam String keyword) {
        List<ChatMessage> messages = chatMessageService.searchMessages(roomId, keyword);
        return ResponseEntity.ok(messages);
    }

    // 獲取在線用戶
    @GetMapping("/online/{roomId}")
    public ResponseEntity<Set<Object>> getOnlineUsers(@PathVariable String roomId) {
        Set<Object> users = chatMessageService.getOnlineUsers(roomId);
        return ResponseEntity.ok(users);
    }

    // 獲取在線用戶數量
    @GetMapping("/online/count/{roomId}")
    public ResponseEntity<Long> getOnlineUserCount(@PathVariable String roomId) {
        long count = chatMessageService.getOnlineUserCount(roomId);
        return ResponseEntity.ok(count);
    }
}
