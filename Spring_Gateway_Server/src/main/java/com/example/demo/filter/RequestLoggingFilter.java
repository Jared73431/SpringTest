package com.example.demo.filter;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 自訂的全域 filter：記錄每個經過 Gateway 的請求，例如
 * <pre>
 * GET /books-service/api/books -> http://172.25.0.5:8084/api/books 200 OK (12 ms)
 * </pre>
 * GlobalFilter 會套用到所有路由，不需要在 YAML 設定；只對「有符合路由」的請求生效。
 *
 * Gateway 建立在 WebFlux（非阻塞）之上，filter 回傳 Mono：
 * chain.filter(exchange) 把請求交給後面的 filter 與後端，doFinally 在整個請求結束後（成功或失敗）才執行。
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		long start = System.nanoTime();
		return chain.filter(exchange).doFinally(signal -> {
			// 實際轉送的網址；lb:// 路由會是 LoadBalancer 選中的那一台
			URI target = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			log.info("{} {} -> {} {} ({} ms)", exchange.getRequest().getMethod(),
					exchange.getRequest().getURI().getRawPath(), target, exchange.getResponse().getStatusCode(),
					elapsedMs);
		});
	}

	// 數字越小越先執行；最先執行才能把整段處理時間都算進去
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
