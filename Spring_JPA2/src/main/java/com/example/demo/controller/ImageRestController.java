package com.example.demo.controller;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ImageResponse;
import com.example.demo.entity.Image;
import com.example.demo.service.ImageService;

@RestController
@RequestMapping("/api/images")
public class ImageRestController {

    private final ImageService imageService;

    public ImageRestController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        Image savedImage = imageService.storeImage(file);
        return ResponseEntity.created(URI.create("/api/images/" + savedImage.getId()))
                .body(ImageResponse.from(savedImage));
    }

    // 只回傳摘要資訊，不含圖片內容
    @GetMapping
    public List<ImageResponse> getAllImages() {
        return imageService.getAllImages();
    }

    // 回傳圖片本身（Content-Type 為上傳時的檔案類型），頁面的 <img> 也使用這個網址
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        Image image = imageService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .contentLength(image.getData().length)
                .body(image.getData());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
