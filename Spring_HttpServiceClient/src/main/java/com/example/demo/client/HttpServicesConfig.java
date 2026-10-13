package com.example.demo.client;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * 註冊 HTTP Service Client：Spring 為 BookClient 產生實作並宣告成 Bean，Controller 直接注入即可。
 *
 * <p>
 * group 是設定的名稱：spring.http.serviceclient.book-server.base-url、connect-timeout… 會套用到這個群組的所有介面。
 * 同一個服務有多個介面時，可以放在同一個群組，共用位址與設定。
 */
@Configuration
@ImportHttpServices(group = "book-server", types = BookClient.class)
public class HttpServicesConfig {
}
