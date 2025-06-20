package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Message;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 獲取房間最近的消息
    List<Message> findByRoomOrderByCreatedAtDesc(Room room, Pageable pageable);

    // 獲取兩個用戶之間的私人消息
    @Query("SELECT m FROM Message m WHERE m.isPrivate = true AND " +
            "((m.sender = :user1 AND m.recipient = :user2) OR (m.sender = :user2 AND m.recipient = :user1)) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findPrivateMessagesBetweenUsers(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);

    // 獲取用戶的所有私人消息
    @Query("SELECT m FROM Message m WHERE m.isPrivate = true AND (m.sender = :user OR m.recipient = :user) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findUserPrivateMessages(@Param("user") User user, Pageable pageable);

    // 統計房間消息數量
    long countByRoom(Room room);

    // 刪除指定時間之前的消息
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
