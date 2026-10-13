package com.example.demo.chat;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.demo.chat.ChatMessage.Type;

/**
 * 斷線（關掉分頁、網路中斷、客戶端 DISCONNECT）時，從所有聊天室移除，並廣播 LEAVE 與新的名單。
 */
@Component
public class PresenceEventListener {

	private final RoomPresence presence;

	private final SimpMessagingTemplate messagingTemplate;

	public PresenceEventListener(RoomPresence presence, SimpMessagingTemplate messagingTemplate) {
		this.presence = presence;
		this.messagingTemplate = messagingTemplate;
	}

	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		if (event.getUser() == null) {
			return;
		}
		String user = event.getUser().getName();
		for (String room : presence.leaveAll(user)) {
			messagingTemplate.convertAndSend("/topic/rooms/" + room, ChatMessage.system(room, Type.LEAVE, user));
			messagingTemplate.convertAndSend("/topic/rooms/" + room + "/users", presence.users(room));
		}
	}
}
