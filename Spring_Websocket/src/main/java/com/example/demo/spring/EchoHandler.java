package com.example.demo.spring;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Spring 的 Echo，行為和 jakarta.EchoEndpoint 相同。
 *
 * <p>
 * <b>所有連線共用這一個物件</b>（Spring Bean 預設是 singleton）：連線用參數傳進來，<b>不能</b>存在欄位裡，
 * 否則後連進來的會蓋掉前一個。
 */
@Component
public class EchoHandler extends TextWebSocketHandler {

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		if (message.getPayload().equalsIgnoreCase("bye")) {
			session.close(CloseStatus.NORMAL.withReason("Bye"));
			return;
		}
		session.sendMessage(new TextMessage("[" + Instant.now().toEpochMilli() + "] Hello " + message.getPayload()));
	}
}
