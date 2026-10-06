package com.example.demo.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * 訊息廣播：Sinks 是 Hot 的資料來源（第 2 課），一個人發送，所有正在 SSE 連線的人都會收到。
 *
 * <p>
 * 訊息存在記憶體中，重新啟動就會消失；多個實例之間也不會互通（需要 Redis Pub/Sub 之類的機制）。
 */
@Service
public class MessageService {

	// directBestEffort：只送給目前連線中的訂閱者；沒有人連線時的訊息不保留（歷史訊息改由 findAll 查詢）
	private final Sinks.Many<Message> sink = Sinks.many().multicast().directBestEffort();
	private final Map<Long, Message> messages = new ConcurrentHashMap<>();
	private final AtomicLong ids = new AtomicLong();

	public Mono<Message> send(String text) {
		return Mono.fromCallable(() -> {
			var message = new Message(ids.incrementAndGet(), text, Instant.now());
			messages.put(message.id(), message);
			// 多個請求可能同時呼叫 emitNext；Sinks 不允許同時送出（FAIL_NON_SERIALIZED），busyLooping 會短暫重試
			sink.emitNext(message, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100)));
			return message;
		});
	}

	public Flux<Message> stream() {
		return sink.asFlux();
	}

	public Flux<Message> findAll() {
		return Flux.fromIterable(messages.values()).sort((a, b) -> Long.compare(a.id(), b.id()));
	}

	/** 查不到時是 Mono.empty()，不是 null，也不是錯誤（第 5 課） */
	public Mono<Message> findById(long id) {
		return Mono.justOrEmpty(messages.get(id));
	}
}
