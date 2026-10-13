package com.example.demo.chat;

import java.security.Principal;
import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import com.example.demo.chat.ChatMessage.Type;

/**
 * STOMP 的「Controller」：處理客戶端 SEND 到 /app/... 的訊息。
 *
 * <pre>
 * SEND /app/rooms/{room}/join       加入聊天室 → /topic/rooms/{room}（JOIN）、/topic/rooms/{room}/users（名單）
 * SEND /app/rooms/{room}/messages   發言 → 存進資料庫 → /topic/rooms/{room}
 * SEND /app/private                 私訊 → 對方與自己的 /user/queue/private
 * 發生錯誤                           → 只有發送者的 /user/queue/errors
 * </pre>
 *
 * 發送者一律用 Principal（連線時決定），不從訊息內容讀取：修正前 sender 由客戶端自己填，任何人都能冒充別人。
 */
@Controller
public class ChatController {

	private static final Pattern VALID_ROOM = Pattern.compile("[a-z0-9-]{1,50}");

	static final int MAX_CONTENT_LENGTH = 500;

	private final ChatMessageRepository repository;

	private final RoomPresence presence;

	private final SimpMessagingTemplate messagingTemplate;

	private final SimpUserRegistry userRegistry;

	public ChatController(ChatMessageRepository repository, RoomPresence presence,
			SimpMessagingTemplate messagingTemplate, SimpUserRegistry userRegistry) {
		this.repository = repository;
		this.presence = presence;
		this.messagingTemplate = messagingTemplate;
		this.userRegistry = userRegistry;
	}

	public record SendMessage(String content) {
	}

	public record PrivateRequest(String to, String content) {
	}

	public record PrivateMessage(String from, String to, String content, Instant sentAt) {
	}

	@MessageMapping("/rooms/{room}/join")
	public void join(@DestinationVariable String room, Principal user) {
		requireValidRoom(room);
		var users = presence.join(room, user.getName());
		messagingTemplate.convertAndSend("/topic/rooms/" + room, ChatMessage.system(room, Type.JOIN, user.getName()));
		messagingTemplate.convertAndSend("/topic/rooms/" + room + "/users", users);
	}

	// 回傳值送到 @SendTo 的目的地：訂閱這個聊天室的每個人都會收到
	@MessageMapping("/rooms/{room}/messages")
	@SendTo("/topic/rooms/{room}")
	public ChatMessage send(@DestinationVariable String room, @Payload SendMessage message, Principal user) {
		requireValidRoom(room);
		return repository.save(room, user.getName(), requireValidContent(message.content()));
	}

	/**
	 * convertAndSendToUser(對方的名稱, ...)：Spring 依名稱找到對方的連線（SimpUserRegistry），送到他訂閱的 /user/queue/private。
	 * 修正前的 Spring_Websocket3 沒有 Principal，所以私訊永遠送不到。
	 */
	@MessageMapping("/private")
	public void sendPrivate(@Payload PrivateRequest request, Principal user) {
		if (request.to() == null || userRegistry.getUser(request.to()) == null) {
			throw new IllegalArgumentException("User is offline: " + request.to());
		}
		var message = new PrivateMessage(user.getName(), request.to(), requireValidContent(request.content()),
				Instant.now());
		messagingTemplate.convertAndSendToUser(request.to(), "/queue/private", message);
		messagingTemplate.convertAndSendToUser(user.getName(), "/queue/private", message); // 自己的畫面也要顯示
	}

	/**
	 * 錯誤送回發送者（broadcast = false：只送給發生錯誤的那個連線）。
	 * 修正前的 Spring_Websocket4 只寫 log，發送者不知道訊息沒送出。
	 */
	@MessageExceptionHandler(IllegalArgumentException.class)
	@SendToUser(destinations = "/queue/errors", broadcast = false)
	public String handleError(IllegalArgumentException e) {
		return e.getMessage();
	}

	private static void requireValidRoom(String room) {
		if (!VALID_ROOM.matcher(room).matches()) {
			throw new IllegalArgumentException("Invalid room: " + room);
		}
	}

	private static String requireValidContent(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("Message must not be blank");
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("Message is longer than " + MAX_CONTENT_LENGTH + " characters");
		}
		return content;
	}
}
