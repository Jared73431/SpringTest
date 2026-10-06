package com.example.demo.reactor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

/**
 * 第 7 課：Context 取代 ThreadLocal。
 *
 * <p>
 * 直覺以為：用 ThreadLocal（例如 MDC、Spring Security 的 SecurityContextHolder）傳遞 traceId、登入者。<br>
 * 實際上：第 5 課看到一個請求會在多個執行緒之間切換，ThreadLocal 會遺失。Reactor 的做法是 Context：
 * 綁在「這一次訂閱」上，而不是綁在執行緒上。
 *
 * <p>
 * 反直覺的地方：Context 是從 Subscriber 往上游傳的，所以 contextWrite 只對寫在它「上面」的操作子有效。
 * 在 WebFlux 中通常由 WebFilter 在最外層寫入，Controller 裡的程式就都讀得到。
 */
class L07_ContextTest {

	private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

	@AfterEach
	void clearThreadLocal() {
		TRACE_ID.remove();
	}

	@Test
	void threadLocal_shouldBeLost_whenSwitchingThreads() {
		TRACE_ID.set("abc-123");

		Mono<String> mono = Mono.just("請求")
				.publishOn(Schedulers.boundedElastic())
				.map(s -> String.valueOf(TRACE_ID.get())); // 換到另一個執行緒，讀不到

		StepVerifier.create(mono).expectNext("null").verifyComplete();
	}

	@Test
	void context_shouldSurviveThreadSwitch() {
		Mono<String> mono = Mono.just("請求")
				.publishOn(Schedulers.boundedElastic())
				.flatMap(s -> Mono.deferContextual(ctx -> Mono.just(ctx.<String>get("traceId"))))
				.contextWrite(Context.of("traceId", "abc-123"));

		StepVerifier.create(mono).expectNext("abc-123").verifyComplete();
	}

	// contextWrite 寫在下面，只有上面的操作子讀得到；寫在上面，下面的操作子讀不到
	@Test
	void context_shouldOnlyBeVisibleAboveContextWrite() {
		Mono<String> mono = Mono.just("請求")
				.contextWrite(Context.of("traceId", "abc-123")) // 寫在讀取的上面
				.flatMap(s -> Mono.deferContextual(ctx -> Mono.just(ctx.getOrDefault("traceId", "讀不到"))));

		StepVerifier.create(mono).expectNext("讀不到").verifyComplete();
	}

	// Context 是不可變的：每次 contextWrite 產生新的 Context，離讀取位置最近（最下游往上第一個）的寫入優先
	@Test
	void contextWrite_closestToReaderShouldWin() {
		Mono<String> mono = Mono.deferContextual(ctx -> Mono.just(ctx.<String>get("user")))
				.contextWrite(Context.of("user", "內層"))
				.contextWrite(Context.of("user", "外層"));

		StepVerifier.create(mono).expectNext("內層").verifyComplete();
	}
}
