package com.example.demo.config;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.chat.ChatUser;

/**
 * STOMP 的設定。
 *
 * <pre>
 * 客戶端 SEND  /app/...    → @MessageMapping 處理（ChatController）
 * 客戶端 SUBSCRIBE /topic/... → broker 廣播給所有訂閱者
 * 客戶端 SUBSCRIBE /user/queue/... → 只收到給「自己」的訊息（私訊、錯誤）
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{N}_-]{1,20}");

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// Spring 內建的 broker（在記憶體中）：只適合一台主機，多台時改用 RabbitMQ 等外部 broker
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		// 送給同一個連線的訊息依序送出（第 4 課）
		registry.setPreservePublishOrder(true);
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 沒有 setAllowedOriginPatterns("*")：預設只允許同源（修正前任何網站都能連，見 Spring_Websocket 第 3 課）
		// 沒有 withSockJS()：現代瀏覽器都支援 WebSocket，SockJS 是給舊瀏覽器的備援
		registry.addEndpoint("/ws")
				.addInterceptors(new RequireNameInterceptor())
				.setHandshakeHandler(new NameHandshakeHandler());
		// 同一個連線送來的訊息依序處理：預設由執行緒池平行處理，連續送出的兩則可能顛倒（第 4 課）
		registry.setPreserveReceiveOrder(true);
	}

	/** 握手時檢查 ?name=：沒有或不合法就拒絕連線（400） */
	static class RequireNameInterceptor implements HandshakeInterceptor {

		@Override
		public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler wsHandler, Map<String, Object> attributes) {
			String name = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("name");
			if (name == null || !VALID_NAME.matcher(name).matches()) {
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				return false;
			}
			attributes.put("name", name);
			return true;
		}

		@Override
		public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
				Exception exception) {
		}
	}

	/**
	 * 把名稱設成這條連線的 Principal：私訊（convertAndSendToUser）、@SendToUser 都靠它找到連線。
	 *
	 * <p>
	 * 練習用：名稱由使用者自己填，沒有密碼。真實系統由 Spring Security 驗證身分後決定 Principal（見 Spring_Security3）。
	 */
	static class NameHandshakeHandler extends DefaultHandshakeHandler {

		@Override
		protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
				Map<String, Object> attributes) {
			return new ChatUser((String) attributes.get("name"));
		}
	}
}
