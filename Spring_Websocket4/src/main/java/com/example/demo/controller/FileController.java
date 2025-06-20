package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.Message;
import com.example.demo.entity.MessageAttachment;
import com.example.demo.service.ChatService;
import com.example.demo.service.FileUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FileController {

    private final FileUploadService fileUploadService;
    private final ChatService chatService; // 使用 ChatService 替代 MessageService
    private final ObjectMapper objectMapper;

    /**
     * 文件上傳接口
     */
    @PostMapping("/chat/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("messageData") String messageData) {

        try {
            log.info("收到文件上傳請求，文件名: {}, 大小: {} bytes",
                    file.getOriginalFilename(), file.getSize());

            // 解析 messageData JSON
            Map<String, Object> messageMap = objectMapper.readValue(messageData, Map.class);

            // 創建或獲取 Message 對象
            Message message = createOrGetMessage(messageMap);

            // 上傳文件並創建附件記錄
            MessageAttachment attachment = fileUploadService.uploadFile(file, message);

            // 構建響應數據
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件上傳成功");
            response.put("data", buildAttachmentResponse(attachment));

            log.info("文件上傳成功: {}", attachment.getStoredFilename());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("文件上傳驗證失敗: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (IOException e) {
            log.error("文件上傳IO異常: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "文件上傳失敗");

        } catch (Exception e) {
            log.error("文件上傳未知異常: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系統異常");
        }
    }

    @GetMapping("/files/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            File file = fileUploadService.getFile(filename);
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("下載文件失敗: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/files/thumbnail/{filename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        File file = fileUploadService.getThumbnail(filename);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    /**
     * 創建或獲取 Message 對象
     */
    private Message createOrGetMessage(Map<String, Object> messageMap) {
        String senderUsername = (String) messageMap.get("senderUsername");
        String content = (String) messageMap.getOrDefault("content", "發送了一個文件");
        Boolean isPrivate = (Boolean) messageMap.getOrDefault("isPrivate", false);
        String recipientUsername = (String) messageMap.get("recipientUsername");
        String roomId = (String) messageMap.get("roomId");

        if (senderUsername == null) {
            throw new IllegalArgumentException("發送者用戶名不能為空");
        }

        // 使用 ChatService 創建文件消息
        return chatService.createFileMessage(senderUsername, content, isPrivate, recipientUsername, roomId);
    }

    /**
     * 構建附件響應數據
     */
    private Map<String, Object> buildAttachmentResponse(MessageAttachment attachment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", attachment.getId());
        data.put("originalFilename", attachment.getOriginalFilename());
        data.put("storedFilename", attachment.getStoredFilename());
        data.put("fileSize", attachment.getFileSize());
        data.put("contentType", attachment.getContentType());
        data.put("downloadUrl", attachment.getDownloadUrl());
        data.put("isImage", attachment.getIsImage());

        if (attachment.getThumbnailUrl() != null) {
            data.put("thumbnailUrl", attachment.getThumbnailUrl());
        }

        return data;
    }

    /**
     * 構建錯誤響應
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(status).body(error);
    }
}
