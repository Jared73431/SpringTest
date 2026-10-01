package com.example.demo.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.ImageService;

/**
 * 圖片上傳頁面（Thymeleaf）。頁面上的圖片透過 GET /api/images/{id} 顯示。
 */
@Controller
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("images", imageService.getAllImages());
        return "index";
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "請選擇一個檔案");
            return "redirect:/";
        }

        try {
            imageService.storeImage(file);
            redirectAttributes.addFlashAttribute("message", "圖片上傳成功!");
        } catch (IOException e) {
            log.warn("圖片上傳失敗: {}", file.getOriginalFilename(), e);
            redirectAttributes.addFlashAttribute("message", "圖片上傳失敗: " + e.getMessage());
        }

        return "redirect:/";
    }

    // 刪除會修改資料，使用 POST（修正前為 GET /delete/{id}）
    @PostMapping("/images/{id}/delete")
    public String deleteImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            imageService.deleteImage(id);
            redirectAttributes.addFlashAttribute("message", "圖片刪除成功!");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("message", "圖片刪除失敗: " + e.getMessage());
        }
        return "redirect:/";
    }
}
