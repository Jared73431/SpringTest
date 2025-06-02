package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.entity.ChatMessage;
import com.example.demo.repository.ChatMessageRepository;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 儲存訊息到資料庫
    public ChatMessage saveMessage(ChatMessage message) {
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 清除快取，讓下次查詢時重新載入
        redisTemplate.delete("recent_messages:" + message.getRoomId());

        return savedMessage;
    }

    // 獲取最近訊息（使用快取）
    @Cacheable(value = "recentMessages", key = "#roomId")
    public List<ChatMessage> getRecentMessages(String roomId) {
        return chatMessageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(roomId);
    }

    // 分頁獲取歷史訊息
    public Page<ChatMessage> getMessageHistory(String roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
    }

    // 搜尋訊息
    public List<ChatMessage> searchMessages(String roomId, String keyword) {
        return chatMessageRepository.searchMessages(roomId, keyword);
    }

    // 管理在線用戶（使用 Redis）
    public void addOnlineUser(String roomId, String username) {
        String key = "online_users:" + roomId;
        redisTemplate.opsForSet().add(key, username);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    public void removeOnlineUser(String roomId, String username) {
        String key = "online_users:" + roomId;
        redisTemplate.opsForSet().remove(key, username);
    }

    public Set<Object> getOnlineUsers(String roomId) {
        String key = "online_users:" + roomId;
        return redisTemplate.opsForSet().members(key);
    }

    public long getOnlineUserCount(String roomId) {
        String key = "online_users:" + roomId;
        return redisTemplate.opsForSet().size(key);
    }

    // 定期清理過期訊息
    public void cleanupOldMessages(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        chatMessageRepository.deleteByCreatedAtBefore(cutoffTime);
    }
}
