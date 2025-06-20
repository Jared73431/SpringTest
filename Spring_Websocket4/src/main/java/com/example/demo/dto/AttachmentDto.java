package com.example.demo.dto;

import lombok.Data;

@Data
public class AttachmentDto {
    private Long id;
    private String originalFilename;
    private String downloadUrl;
    private String thumbnailUrl;
    private Boolean isImage;
    private Long fileSize;
    private String contentType;
}
