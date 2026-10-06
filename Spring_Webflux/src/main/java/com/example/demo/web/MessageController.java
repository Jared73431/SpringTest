package com.example.demo.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

	private final MessageService messageService;

	public MessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	@PostMapping
	public Mono<ResponseEntity<Message>> send(@Valid @RequestBody MessageRequest request) {
		return messageService.send(request.text())
				.map(message -> ResponseEntity.created(URI.create("/api/messages/" + message.id())).body(message));
	}

	// 同一個 URL：Accept 是 JSON 時回傳歷史訊息，是 text/event-stream 時即時推播新訊息
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<Message> findAll() {
		return messageService.findAll();
	}

	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<Message>> stream() {
		// 連線後先送一個註解事件：讓客戶端立刻收到回應標頭（有些 proxy 也需要有資料流動才不會斷線）。
		// merge 會依序訂閱：先訂閱 Sinks，再送出註解，所以客戶端收到註解時，之後的訊息一定收得到
		return Flux.merge(
				messageService.stream().map(message -> ServerSentEvent.builder(message)
						.id(String.valueOf(message.id()))
						.event("message")
						.build()),
				Mono.just(ServerSentEvent.<Message>builder().comment("connected").build()));
	}

	// ✅ 查不到時回傳 404（ProblemDetail）
	@GetMapping("/{id}")
	public Mono<Message> findById(@PathVariable long id) {
		return messageService.findById(id)
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到訊息：" + id)));
	}

	// ❌ 教學用：沒有 switchIfEmpty。查不到時 Mono 是空的，WebFlux 回傳 200 且沒有內容，客戶端無法分辨「查不到」
	@GetMapping("/{id}/unchecked")
	public Mono<Message> findByIdUnchecked(@PathVariable long id) {
		return messageService.findById(id);
	}
}
