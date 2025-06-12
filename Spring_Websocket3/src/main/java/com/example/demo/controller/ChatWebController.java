package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ChatWebController {

    @GetMapping("/chat")
    public String chatPage(Model model, HttpServletRequest request) {
        // 可以從 session 或其他地方獲取用戶信息
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return "chat"; // 返回 chat.html
    }
}
