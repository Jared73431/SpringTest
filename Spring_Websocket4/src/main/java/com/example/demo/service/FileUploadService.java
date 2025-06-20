package com.example.demo.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.FileUploadProperties;
import com.example.demo.entity.Message;
import com.example.demo.entity.MessageAttachment;
import com.example.demo.repository.MessageAttachmentRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileUploadService {

    private final FileUploadProperties properties;
    private final MessageAttachmentRepository attachmentRepository;

    private static final List<String> IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final int THUMBNAIL_SIZE = 200;

    @Autowired
    public FileUploadService(FileUploadProperties properties,
                             MessageAttachmentRepository attachmentRepository) {
        this.properties = properties;
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * 上傳文件
     */
    public MessageAttachment uploadFile(MultipartFile file, Message message) throws IOException {
        log.info("開始上傳文件: {}", file.getOriginalFilename());

        // 驗證文件
        validateFile(file);

        // 獲取配置的上傳目錄
        String baseDir = properties.getDir();
        log.info("配置的上傳目錄: {}", baseDir);

        // 如果是相對路徑，轉換為絕對路徑
        if (!Paths.get(baseDir).isAbsolute()) {
            baseDir = System.getProperty("user.dir") + File.separator + baseDir;
            log.info("轉換為絕對路徑: {}", baseDir);
        }

        // 創建基礎上傳目錄
        Path basePath = Paths.get(baseDir);
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
            log.info("創建基礎目錄: {}", basePath);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String storedFilename = generateUniqueFilename(extension);

        // 構建日期路徑
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadPath = basePath.resolve(datePath);

        // 創建日期目錄（關鍵步驟）
        try {
            Files.createDirectories(uploadPath);
            log.info("創建日期目錄: {}", uploadPath);
        } catch (IOException e) {
            log.error("創建目錄失敗: {}", uploadPath, e);
            throw new IOException("無法創建上傳目錄: " + uploadPath, e);
        }

        // 構建完整文件路徑
        Path filePath = uploadPath.resolve(storedFilename);
        log.info("目標文件路徑: {}", filePath);

        // 檢查父目錄是否存在
        if (!Files.exists(filePath.getParent())) {
            log.error("父目錄不存在: {}", filePath.getParent());
            throw new IOException("父目錄不存在: " + filePath.getParent());
        }

        // 保存文件
        try {
            file.transferTo(filePath.toFile());
            log.info("文件保存成功: {}", filePath);
        } catch (IOException e) {
            log.error("文件保存失敗: {}", filePath, e);
            throw new IOException("文件保存失敗: " + filePath, e);
        }

        // 創建附件記錄
        MessageAttachment attachment = new MessageAttachment();
        attachment.setMessage(message);
        attachment.setOriginalFilename(originalFilename);
        attachment.setStoredFilename(storedFilename);
        attachment.setFilePath(filePath.toString());
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setDownloadUrl("/api/files/download/" + storedFilename);
        attachment.setIsImage(isImageFile(file.getContentType()));

        // 如果是圖片，生成縮略圖
        if (attachment.getIsImage()) {
            String thumbnailUrl = generateThumbnail(filePath, storedFilename, datePath);
            attachment.setThumbnailUrl(thumbnailUrl);
        }

        // 保存到數據庫
        return attachmentRepository.save(attachment);
    }

    /**
     * 驗證文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能為空");
        }

        if (file.getSize() > properties.getMaxSize()) {
            throw new IllegalArgumentException("文件大小超過限制: " + formatFileSize(properties.getMaxSize()));
        }

        if (!properties.getAllowedTypes().contains(file.getContentType())) {
            throw new IllegalArgumentException("不支援的文件類型: " + file.getContentType());
        }
    }

    /**
     * 創建上傳目錄
     */
    private void createUploadDirectories() throws IOException {
        Path uploadPath = Paths.get(properties.getDir());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path thumbnailPath = Paths.get(properties.getDir(), "thumbnails");
        if (!Files.exists(thumbnailPath)) {
            Files.createDirectories(thumbnailPath);
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFilename(String extension) {
        return UUID.randomUUID().toString() +
                "_" + System.currentTimeMillis() +
                (extension != null && !extension.isEmpty() ? "." + extension : "");
    }

    /**
     * 判斷是否為圖片文件
     */
    private boolean isImageFile(String contentType) {
        return contentType != null && IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * 生成縮略圖
     */
    private String generateThumbnail(Path originalPath, String storedFilename, String datePath) {
        try {
            BufferedImage originalImage = ImageIO.read(originalPath.toFile());
            if (originalImage == null) {
                return null;
            }

            // 生成縮略圖
            BufferedImage thumbnail = Scalr.resize(originalImage,
                    Scalr.Method.QUALITY,
                    Scalr.Mode.AUTOMATIC,
                    THUMBNAIL_SIZE,
                    THUMBNAIL_SIZE);

            // 保存縮略圖
            String thumbnailFilename = "thumb_" + storedFilename;
            Path thumbnailDir = Paths.get(properties.getDir(), "thumbnails", datePath);
            Files.createDirectories(thumbnailDir);
            Path thumbnailPath = thumbnailDir.resolve(thumbnailFilename);

            String format = FilenameUtils.getExtension(storedFilename).toLowerCase();
            if (format.equals("jpg")) format = "jpeg";

            ImageIO.write(thumbnail, format, thumbnailPath.toFile());

            return "/api/files/thumbnail/" + thumbnailFilename;

        } catch (IOException e) {
            log.error("生成縮略圖失敗: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 獲取文件
     */
    public File getFile(String filename) {
        // 在日期目錄中查找文件
        String[] datePaths = {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        };

        for (String datePath : datePaths) {
            Path filePath = Paths.get(properties.getDir(), datePath, filename);
            File file = filePath.toFile();
            if (file.exists()) {
                return file;
            }
        }

        // 如果在日期目錄中找不到，在根目錄查找
        Path rootPath = Paths.get(properties.getDir(), filename);
        File rootFile = rootPath.toFile();
        if (rootFile.exists()) {
            return rootFile;
        }

        return null;
    }

    /**
     * 獲取縮略圖
     */
    public File getThumbnail(String filename) {
        String[] datePaths = {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        };

        for (String datePath : datePaths) {
            Path thumbnailPath = Paths.get(properties.getDir(), "thumbnails", datePath, filename);
            File file = thumbnailPath.toFile();
            if (file.exists()) {
                return file;
            }
        }

        return null;
    }

    /**
     * 刪除文件
     */
    public void deleteFile(MessageAttachment attachment) {
        try {
            // 刪除原文件
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);

            // 刪除縮略圖
            if (attachment.getThumbnailUrl() != null) {
                String thumbnailFilename = "thumb_" + attachment.getStoredFilename();
                File thumbnail = getThumbnail(thumbnailFilename);
                if (thumbnail != null && thumbnail.exists()) {
                    thumbnail.delete();
                }
            }

            // 從數據庫刪除記錄
            attachmentRepository.delete(attachment);

        } catch (IOException e) {
            log.error("刪除文件失敗: {}", e.getMessage());
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
