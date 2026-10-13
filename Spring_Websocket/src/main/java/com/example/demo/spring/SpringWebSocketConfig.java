package com.example.demo.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Spring 的 WebSocketHandler 註冊在哪個路徑。
 *
 * <p>
 * 沒有呼叫 setAllowedOrigins(...)：預設<b>只允許同源</b>（頁面和 WebSocket 在同一個網址），其他網站的頁面連線會被拒絕（403）。
 * 修正前的 Spring_Websocket4 設成 setAllowedOrigins("*")，任何網站都能連進來（第 3 課）。
 */
@Configuration
@EnableWebSocket
public class SpringWebSocketConfig implements WebSocketConfigurer {

	private final EchoHandler echoHandler;

	private final BroadcastHandler broadcastHandler;

	public SpringWebSocketConfig(EchoHandler echoHandler, BroadcastHandler broadcastHandler) {
		this.echoHandler = echoHandler;
		this.broadcastHandler = broadcastHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(echoHandler, "/ws/spring/echo");
		registry.addHandler(broadcastHandler, "/ws/spring/broadcast");
	}
}
