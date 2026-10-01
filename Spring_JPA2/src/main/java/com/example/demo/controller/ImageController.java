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
 * 與 REST Controller 不同，這裡是表單流程：錯誤在本類別 catch 後以 Flash Attribute 顯示訊息並 redirect，
 * 而不是交給 GlobalExceptionHandler 回傳 ProblemDetail（瀏覽器使用者看不懂 JSON 錯誤）。
 */
@Controller
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /** GET /：顯示上傳頁面與圖片列表（只帶中繼資料，圖片本身由 &lt;img&gt; 另外請求）。 */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("images", imageService.getAllImages());
        return "index";
    }

    /**
     * POST /upload：表單上傳圖片，完成後 redirect 回首頁。
     * 使用 Post/Redirect/Get：重新整理頁面時不會重複送出表單；訊息透過 Flash Attribute 在 redirect 後顯示一次。
     */
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

    /**
     * POST /images/{id}/delete：刪除圖片後 redirect 回首頁；圖片不存在時以訊息提示，不回 404 頁面。
     * 刪除會修改資料，使用 POST（修正前為 GET /delete/{id}）：GET 應該是安全、可重複的操作，
     * 否則瀏覽器預先載入或爬蟲點擊連結就可能誤刪資料。HTML 表單只支援 GET / POST，因此不用 DELETE。
     */
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
