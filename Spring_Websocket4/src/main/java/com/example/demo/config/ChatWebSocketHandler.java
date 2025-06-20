package com.example.demo.config;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.demo.dto.MessageDto;
import com.example.demo.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // 儲存用戶會話
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // 儲存會話到用戶名的映射
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = getUsernameFromSession(session);
        if (username != null) {
            // 自動創建或獲取用戶（如果不存在則創建）
            chatService.ensureUserExists(username);

            userSessions.put(username, session);
            sessionUsers.put(session.getId(), username);

            // 更新用戶在線狀態
            chatService.updateUserOnlineStatus(username, true);

            log.info("用戶 {} 連接到WebSocket", username);

            // 廣播用戶上線消息
            broadcastUserStatusChange(username, true);
        }

    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

        try {
            String payload = (String) message.getPayload();
            MessageDto messageDto = objectMapper.readValue(payload, MessageDto.class);

            // 保存消息到數據庫
            MessageDto savedMessage = chatService.saveMessage(messageDto);

            // 根據消息類型進行不同處理
            if (savedMessage.getIsPrivate()) {
                // 私人消息
                sendPrivateMessage(savedMessage);
            } else {
                // 群組消息
                broadcastMessage(savedMessage);
            }

        } catch (Exception e) {
            log.error("處理消息失敗: {}", e.getMessage());
        }

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket傳輸錯誤: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

        String username = sessionUsers.remove(session.getId());
        if (username != null) {
            userSessions.remove(username);

            // 更新用戶離線狀態
            chatService.updateUserOnlineStatus(username, false);

            log.info("用戶 {} 斷開WebSocket連接", username);

            // 廣播用戶下線消息
            broadcastUserStatusChange(username, false);
        }

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String getUsernameFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null && query.startsWith("username=")) {
                return query.substring(9);
            }
        }
        return null;
    }

    private void sendPrivateMessage(MessageDto message) {
        WebSocketSession recipientSession = userSessions.get(message.getRecipientUsername());
        if (recipientSession != null && recipientSession.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                recipientSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("發送私人消息失敗: {}", e.getMessage());
            }
        }

        // 也發送給發送者確認
        WebSocketSession senderSession = userSessions.get(message.getSenderUsername());
        if (senderSession != null && senderSession.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                senderSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("發送消息確認失敗: {}", e.getMessage());
            }
        }
    }

    private void broadcastMessage(MessageDto message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化消息失敗: {}", e.getMessage());
            return;
        }

        userSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("廣播消息失敗: {}", e.getMessage());
                }
            }
        });
    }

    private void broadcastUserStatusChange(String username, boolean online) {
        Map<String, Object> statusMessage = Map.of(
                "type", "userStatus",
                "username", username,
                "online", online
        );

        try {
            String json = objectMapper.writeValueAsString(statusMessage);
            userSessions.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        log.error("廣播用戶狀態失敗: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("序列化用戶狀態消息失敗: {}", e.getMessage());
        }
    }
}
