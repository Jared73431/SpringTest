package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RoomDto {
    private Long id;
    private String roomId;
    private String name;
    private String description;
    private Integer maxUsers;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
