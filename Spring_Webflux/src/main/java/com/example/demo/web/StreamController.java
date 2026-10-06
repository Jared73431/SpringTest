package com.example.demo.web;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import reactor.core.publisher.Flux;

/**
 * 回傳 Flux 時，HTTP 回應的樣子取決於 Content-Type：
 *
 * <pre>
 * application/json        等全部資料產生完，一次回傳 JSON 陣列（和 MVC 回傳 List 一樣）
 * application/x-ndjson    每產生一筆就送出一行 JSON，連線保持到串流結束
 * text/event-stream       SSE：瀏覽器原生支援（EventSource），適合伺服器主動推播
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class StreamController {

	private final Duration numberDelay;
	private final Duration tickInterval;

	public StreamController(@Value("${demo.numbers.delay:200ms}") Duration numberDelay,
			@Value("${demo.ticks.interval:1s}") Duration tickInterval) {
		this.numberDelay = numberDelay;
		this.tickInterval = tickInterval;
	}

	// 同一個方法支援兩種格式，由請求的 Accept 決定
	@GetMapping(value = "/numbers", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE })
	public Flux<Square> numbers(@RequestParam(defaultValue = "5") @Min(1) @Max(100) int count) {
		return Flux.range(1, count)
				.delayElements(numberDelay) // 模擬每筆資料要花一點時間產生（例如逐筆查詢）
				.map(i -> new Square(i, i * i));
	}

	// 無限串流：客戶端斷線時 WebFlux 會取消訂閱，interval 就停止，不會在背景一直跑
	@GetMapping(value = "/ticks", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<Tick>> ticks() {
		return Flux.interval(tickInterval)
				.map(sequence -> ServerSentEvent.builder(new Tick(sequence, Instant.now()))
						.id(String.valueOf(sequence))
						.event("tick")
						.build());
	}
}
