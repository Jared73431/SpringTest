package com.example.demo.jakarta;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * 在內嵌的 Tomcat 中，@ServerEndpoint 不會自動被找到：用 ServerEndpointExporter 註冊。
 *
 * <p>
 * [Learning] Jakarta 的 endpoint 預設<b>不檢查 Origin</b>，任何網站的頁面都能連進來（第 3 課）。
 * 要限制必須自己寫 ServerEndpointConfig.Configurator 覆寫 checkOrigin；Spring 的 WebSocketHandler 預設只允許同源。
 */
@Configuration
public class JakartaEndpointConfig {

	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		var exporter = new ServerEndpointExporter();
		exporter.setAnnotatedEndpointClasses(EchoEndpoint.class);
		return exporter;
	}
}
