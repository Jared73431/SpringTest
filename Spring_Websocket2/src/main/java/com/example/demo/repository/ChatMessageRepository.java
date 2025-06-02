package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 獲取指定房間的最近訊息
    List<ChatMessage> findTop50ByRoomIdOrderByCreatedAtDesc(String roomId);

    // 分頁獲取房間訊息
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    // 搜尋訊息內容
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.content LIKE %:keyword% ORDER BY cm.createdAt DESC")
    List<ChatMessage> searchMessages(@Param("roomId") String roomId, @Param("keyword") String keyword);

    // 獲取指定時間範圍的訊息
    List<ChatMessage> findByRoomIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String roomId, LocalDateTime startTime, LocalDateTime endTime);

    // 統計指定時間範圍內各用戶的訊息數量
    @Query("SELECT cm.sender, COUNT(cm) FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.createdAt >= :startTime GROUP BY cm.sender ORDER BY COUNT(cm) DESC")
    List<Object[]> getMessageCountBySender(@Param("roomId") String roomId, @Param("startTime") LocalDateTime startTime);

    // 刪除過期訊息（保留指定天數）
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
}
