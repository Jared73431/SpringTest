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

@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image storeImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidRequestException("請選擇要上傳的檔案");
        }
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        byte[] data = file.getBytes();

        Image image = new Image(fileName, contentType, data);
        return imageRepository.save(image);
    }

    public Image getImage(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("圖片", id));
    }

    public List<ImageResponse> getAllImages() {
        return imageRepository.findAllSummaries().stream()
                .map(ImageResponse::from)
                .toList();
    }

    public void deleteImage(Long id) {
        if (!imageRepository.existsById(id)) {
            throw new ResourceNotFoundException("圖片", id);
        }
        imageRepository.deleteById(id);
    }
}
