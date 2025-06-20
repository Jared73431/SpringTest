package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class MessageDto {
    private Long id;
    private String content;
    private String messageType;
    private Boolean isPrivate;
    private String senderUsername;
    private String senderDisplayName;
    private String recipientUsername;
    private String roomId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private List<AttachmentDto> attachments;
}
