package com.example.demo.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ImageResponse;
import com.example.demo.entity.Image;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ImageRepository;

/**
 * 圖片上傳、讀取與刪除；圖片內容以 byte[] 直接存在資料庫。
 * 列表只查詢中繼資料（不含圖片 bytes），避免一次把所有圖片內容載入記憶體。
 */
@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    /**
     * 儲存上傳的圖片。未選擇檔案（空檔案）、不是 PNG / JPEG / GIF / WebP → InvalidRequestException（400）。
     * Content-Type 由檔案內容判斷（ImageFormat），不使用用戶端宣告的值（修正前直接使用，可以偽裝成圖片上傳 HTML）。
     */
    public Image storeImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidRequestException("請選擇要上傳的檔案");
        }
        String fileName = file.getOriginalFilename();
        byte[] data = file.getBytes();
        ImageFormat format = ImageFormat.detect(data)
                .orElseThrow(() -> new InvalidRequestException("只接受 PNG、JPEG、GIF、WebP 圖片"));

        Image image = new Image(fileName, format.contentType(), data);
        return imageRepository.save(image);
    }

    /** 取得單張圖片（含 bytes），供顯示圖片使用。不存在 → ResourceNotFoundException（404）。 */
    public Image getImage(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("圖片", id));
    }

    /** 取得所有圖片的中繼資料（id、檔名、類型、大小、上傳時間），透過投影查詢，不載入圖片內容。 */
    public List<ImageResponse> getAllImages() {
        return imageRepository.findAllSummaries().stream()
                .map(ImageResponse::from)
                .toList();
    }

    /** 刪除圖片。不存在 → ResourceNotFoundException（404），先檢查是為了回傳明確的 404 而非靜默成功。 */
    public void deleteImage(Long id) {
        if (!imageRepository.existsById(id)) {
            throw new ResourceNotFoundException("圖片", id);
        }
        imageRepository.deleteById(id);
    }
}
