package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean isOnline;
    private LocalDateTime lastActive;
}
