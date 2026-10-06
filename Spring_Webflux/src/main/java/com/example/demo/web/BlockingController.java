package com.example.demo.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 阻塞呼叫的錯誤與正確寫法（第 6 課在 WebFlux 中的樣子）。回應中的 thread 欄位顯示阻塞呼叫實際在哪個執行緒上執行。
 *
 * <p>
 * Netty 只有少量的 event loop 執行緒（約等於 CPU 核心數），負責所有連線的讀寫。
 * 在 event loop 上阻塞 200ms，這段時間內同一個執行緒上的其他請求全部都要等；
 * WebFlux 用少量執行緒服務大量連線的前提，就被破壞了。
 */
@RestController
@RequestMapping("/api/blocking")
public class BlockingController {

	public record BlockingResult(String result, String thread) {
	}

	private final Duration legacyDelay;

	public BlockingController(@Value("${demo.blocking.delay:200ms}") Duration legacyDelay) {
		this.legacyDelay = legacyDelay;
	}

	/** 模擬阻塞的舊 API（例如 JDBC、同步的 SDK） */
	private BlockingResult legacyBlockingCall() {
		try {
			Thread.sleep(legacyDelay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return new BlockingResult("完成", Thread.currentThread().getName());
	}

	// 錯誤：阻塞呼叫直接在 Netty 的 event loop（reactor-http-nio-* / reactor-http-epoll-*）上執行。功能正常，但在高流量下會拖慢所有請求
	@GetMapping("/on-event-loop")
	public Mono<BlockingResult> onEventLoop() {
		return Mono.fromCallable(this::legacyBlockingCall);
	}

	// 正確：交給 boundedElastic，event loop 立刻空出來處理其他連線
	@GetMapping("/on-bounded-elastic")
	public Mono<BlockingResult> onBoundedElastic() {
		return Mono.fromCallable(this::legacyBlockingCall).subscribeOn(Schedulers.boundedElastic());
	}
}
