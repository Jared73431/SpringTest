package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByIsOnlineTrue();

    @Modifying
    @Query("UPDATE User u SET u.isOnline = :online, u.lastActive = :lastActive WHERE u.username = :username")
    void updateUserOnlineStatus(@Param("username") String username, @Param("online") Boolean online, @Param("lastActive") LocalDateTime lastActive);

    @Modifying
    @Query("UPDATE User u SET u.lastActive = :lastActive WHERE u.username = :username")
    void updateLastActive(@Param("username") String username, @Param("lastActive") LocalDateTime lastActive);
}
