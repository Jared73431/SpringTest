package com.example.demo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Image;
import com.example.demo.service.ImageService;

@Controller
public class ImageController {

    @Autowired
    private ImageService imageService;

    @GetMapping("/")
    public String home(Model model) {
        List<Image> images = imageService.getAllImages();
        model.addAttribute("images", images);
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
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "圖片上傳失敗: " + e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        Image image = imageService.getImage(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(image.getContentType()));
        headers.setContentLength(image.getData().length);

        return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
    }

    @GetMapping("/delete/{id}")
    public String deleteImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            imageService.deleteImage(id);
            redirectAttributes.addFlashAttribute("message", "圖片刪除成功!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "圖片刪除失敗: " + e.getMessage());
        }
        return "redirect:/";
    }
}
