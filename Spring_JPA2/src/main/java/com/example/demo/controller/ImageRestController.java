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

/**
 * 圖片 REST API：上傳、列表（只含中繼資料）、取得圖片內容、刪除。
 * 不在這裡 try/catch，Service 拋出的例外由 GlobalExceptionHandler 統一轉成 ProblemDetail。
 */
@RestController
@RequestMapping("/api/images")
public class ImageRestController {

    private final ImageService imageService;

    public ImageRestController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * POST /api/images（multipart/form-data，欄位 file）：上傳圖片。
     * 201 + Location，回傳中繼資料；空檔案或缺少 file 欄位 → 400。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        Image savedImage = imageService.storeImage(file);
        return ResponseEntity.created(URI.create("/api/images/" + savedImage.getId()))
                .body(ImageResponse.from(savedImage));
    }

    /** GET /api/images：取得所有圖片的摘要資訊（不含圖片內容，避免回應過大）。200。 */
    @GetMapping
    public List<ImageResponse> getAllImages() {
        return imageService.getAllImages();
    }

    /**
     * GET /api/images/{id}：回傳圖片本身（Content-Type 為上傳時的檔案類型），頁面的 &lt;img&gt; 也使用這個網址。
     * 200；圖片不存在 → 404。
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        Image image = imageService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .contentLength(image.getData().length)
                .body(image.getData());
    }

    /** DELETE /api/images/{id}：刪除圖片。204；圖片不存在 → 404。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
