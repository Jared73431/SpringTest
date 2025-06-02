package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.example.demo.entity.ChatMessage;
import com.example.demo.service.ChatMessageService;

@Controller
public class ChatController {

    @Autowired
    private ChatMessageService chatMessageService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        // 儲存訊息到資料庫
        ChatMessage savedMessage = chatMessageService.saveMessage(chatMessage);
        return savedMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // 添加到在線用戶列表
        chatMessageService.addOnlineUser("public", chatMessage.getSender());

        // 將用戶名添加到WebSocket會話
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());

        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(chatMessage.getSender() + " 加入了聊天室！");

        // 儲存系統訊息
        ChatMessage savedMessage = chatMessageService.saveMessage(chatMessage);
        return savedMessage;
    }
}
