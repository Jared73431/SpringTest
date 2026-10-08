package com.example.demo.message;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * 伺服器端渲染：@Controller（不是 @RestController）的方法回傳的是「view 名稱」，
 * Thymeleaf 依名稱找到 templates/messages.html，把 Model 中的資料填進去，產生完整的 HTML 回傳給瀏覽器。
 */
@Controller
public class MessageController {

	private final MessageService messageService;

	public MessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/messages";
	}

	@GetMapping("/messages")
	public String list(Model model) {
		model.addAttribute("messages", messageService.findAll());
		model.addAttribute("form", new MessageForm());
		return "messages";
	}

	/**
	 * 和 REST API 不同：驗證失敗不回傳 400，而是回到同一頁，顯示錯誤並保留使用者輸入的內容。
	 * 成功時使用 PRG（Post / Redirect / Get）：重新導向到列表頁，使用者按 F5 只會重新 GET，不會重複送出表單。
	 */
	@PostMapping("/messages")
	public String add(@Valid @ModelAttribute("form") MessageForm form, BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("messages", messageService.findAll());
			return "messages";
		}
		messageService.add(form.getAuthor(), form.getContent());
		// flash attribute：只在下一個請求（重新導向後的 GET）存在一次，用來顯示「已送出」之類的訊息
		redirectAttributes.addFlashAttribute("notice", "已送出留言");
		return "redirect:/messages";
	}
}
