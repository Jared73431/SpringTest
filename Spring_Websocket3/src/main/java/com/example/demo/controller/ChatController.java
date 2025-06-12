package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.PrivateMessage;

@Controller
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 用於存儲每個房間的在線用戶
    private static final ConcurrentHashMap<String, Set<String>> roomUsers = new ConcurrentHashMap<>();

    // 用於存儲用戶所在的房間
    private static final ConcurrentHashMap<String, String> userRooms = new ConcurrentHashMap<>();

    // 處理發送消息到特定房間
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable String roomId,
                                   @Payload ChatMessage chatMessage,
                                   SimpMessageHeaderAccessor headerAccessor) {
        // 設置時間戳
        chatMessage.setTimestamp(LocalDateTime.now().toString());
        chatMessage.setRoomId(roomId);

        // 處理用戶加入房間
        if (ChatMessage.MessageType.JOIN.equals(chatMessage.getType())) {
            String sessionId = headerAccessor.getSessionId();
            String username = chatMessage.getSender();

            // 將用戶名存儲在 WebSocket 會話中
            headerAccessor.getSessionAttributes().put("username", username);
            headerAccessor.getSessionAttributes().put("roomId", roomId);

            // 添加用戶到房間
            roomUsers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(username);
            userRooms.put(sessionId, roomId);

            // 廣播在線用戶列表更新
            broadcastOnlineUsers(roomId);

            System.out.println("用戶 " + username + " 加入房間: " + roomId);
        }

        // 處理用戶離開房間
        else if (ChatMessage.MessageType.LEAVE.equals(chatMessage.getType())) {
            String sessionId = headerAccessor.getSessionId();
            String username = chatMessage.getSender();

            // 從房間移除用戶
            Set<String> users = roomUsers.get(roomId);
            if (users != null) {
                users.remove(username);
                if (users.isEmpty()) {
                    roomUsers.remove(roomId);
                }
            }
            userRooms.remove(sessionId);

            // 廣播在線用戶列表更新
            broadcastOnlineUsers(roomId);

            System.out.println("用戶 " + username + " 離開房間: " + roomId);
        }

        // 打印日誌
        System.out.println("收到消息: " + chatMessage.getSender() + " - " + chatMessage.getContent() + " [房間: " + roomId + "]");

        return chatMessage;
    }

    // 兼容舊的公共聊天室路由
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendPublicMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now().toString());
        System.out.println("收到公共消息: " + chatMessage.getSender() + " - " + chatMessage.getContent());
        return chatMessage;
    }

    // 處理用戶加入（兼容舊版本）
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // 將用戶名存儲在 WebSocket 會話中
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("roomId", "public");

        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(chatMessage.getSender() + " 加入了聊天室");
        chatMessage.setTimestamp(LocalDateTime.now().toString());

        // 添加到公共房間
        roomUsers.computeIfAbsent("public", k -> ConcurrentHashMap.newKeySet()).add(chatMessage.getSender());
        broadcastOnlineUsers("public");

        return chatMessage;
    }

    // 發送私人消息
    @MessageMapping("/chat/private")
    public void sendPrivateMessage(@Payload PrivateMessage privateMessage) {
        privateMessage.setTimestamp(LocalDateTime.now().toString());

        // 發送給特定用戶
        messagingTemplate.convertAndSendToUser(
                privateMessage.getRecipient(),
                "/queue/private",
                privateMessage
        );

        System.out.println("私人消息: " + privateMessage.getSender() + " -> " + privateMessage.getRecipient());
    }

    // 發送系統通知
    @MessageMapping("/chat.systemNotification")
    public void sendSystemNotification(@Payload String message) {
        ChatMessage notification = new ChatMessage(
                ChatMessage.MessageType.CHAT,
                message,
                "系統"
        );

        // 手動發送到特定主題
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    // 獲取房間在線用戶
    @MessageMapping("/chat/users/{roomId}")
    public void getRoomUsers(@DestinationVariable String roomId, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Set<String> users = roomUsers.getOrDefault(roomId, ConcurrentHashMap.newKeySet());

        // 發送給請求者
        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/users",
                users
        );
    }

    // 廣播在線用戶列表
    private void broadcastOnlineUsers(String roomId) {
        Set<String> users = roomUsers.getOrDefault(roomId, ConcurrentHashMap.newKeySet());

        // 創建用戶列表消息
        ChatMessage userListMessage = new ChatMessage();
        userListMessage.setType(ChatMessage.MessageType.USER_LIST);
        userListMessage.setContent(String.join(",", users));
        userListMessage.setSender("系統");
        userListMessage.setTimestamp(LocalDateTime.now().toString());

        // 廣播到房間
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/users", users);
    }

    // 處理用戶斷線（由 WebSocketEventListener 調用）
    public void handleUserDisconnect(String sessionId, String username) {
        String roomId = userRooms.get(sessionId);
        if (roomId != null && username != null) {
            Set<String> users = roomUsers.get(roomId);
            if (users != null) {
                users.remove(username);
                if (users.isEmpty()) {
                    roomUsers.remove(roomId);
                }
            }
            userRooms.remove(sessionId);

            // 廣播離開消息
            ChatMessage leaveMessage = new ChatMessage(
                    ChatMessage.MessageType.LEAVE,
                    username + " 離開了聊天室",
                    "系統"
            );

            messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMessage);
            broadcastOnlineUsers(roomId);
        }
    }
}
