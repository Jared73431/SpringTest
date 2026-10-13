package com.example.demo.chat;

import java.security.Principal;

/**
 * 這條連線是誰。連線時（握手）決定一次，之後每則訊息都由伺服器從這裡取得發送者，不信任訊息內容。
 */
public record ChatUser(String name) implements Principal {

	@Override
	public String getName() {
		return name;
	}
}
