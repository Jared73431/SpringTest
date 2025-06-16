package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 啟用簡單消息代理，處理 /topic 和 /queue 前綴的消息
        config.enableSimpleBroker("/topic", "/queue");
        // 設置應用程序目標前綴，客戶端發送消息時使用
        config.setApplicationDestinationPrefixes("/app");
        // 設置用戶目的地前綴，用於點對點消息
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 註冊 STOMP 端點，啟用 SockJS 後備選項
        registry.addEndpoint("/ws").withSockJS();
    }
}
