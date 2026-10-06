package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 第 1 課：什麼都不會發生，直到 subscribe。
 *
 * <p>
 * 直覺以為：呼叫回傳 Mono 的方法，裡面的事情就會做。<br>
 * 實際上：Mono / Flux 只是一份「食譜」（組裝階段 assembly），要有人 subscribe 才會照著做（訂閱階段 subscription）。
 * 在 WebFlux 裡，subscribe 的人是框架：Controller 回傳 Mono，WebFlux 會幫你訂閱。
 */
class L01_LazinessTest {

	private final AtomicInteger calls = new AtomicInteger();

	private String expensiveQuery() {
		calls.incrementAndGet();
		return "結果";
	}

	@Test
	void fromCallable_shouldNotExecute_whenNotSubscribed() {
		Mono<String> mono = Mono.fromCallable(this::expensiveQuery);

		assertThat(calls).hasValue(0); // 只是組裝，什麼都還沒做

		StepVerifier.create(mono).expectNext("結果").verifyComplete(); // StepVerifier 會 subscribe
		assertThat(calls).hasValue(1);
	}

	// Mono.just 的參數是一般的 Java 運算式，在組裝時就已經求值
	@Test
	void just_shouldExecuteImmediately_evenWhenNotSubscribed() {
		Mono<String> mono = Mono.just(expensiveQuery());

		assertThat(calls).hasValue(1); // 沒有人訂閱，查詢卻已經執行了
		assertThat(mono).isNotNull();
	}

	// defer：把「建立 Mono」這件事本身延後到訂閱時
	@Test
	void defer_shouldDelayExecution_untilSubscribed() {
		Mono<String> mono = Mono.defer(() -> Mono.just(expensiveQuery()));

		assertThat(calls).hasValue(0);
		StepVerifier.create(mono).expectNext("結果").verifyComplete();
		assertThat(calls).hasValue(1);
	}

	// 方法裡「return 之前」的程式碼是一般 Java，呼叫方法時就執行；只有 Mono 裡的 lambda 才會延後
	private Mono<String> findUser(AtomicInteger assemblyCalls) {
		assemblyCalls.incrementAndGet(); // 組裝階段：呼叫 findUser 就執行
		return Mono.fromCallable(this::expensiveQuery); // 訂閱階段
	}

	@Test
	void codeBeforeReturn_shouldRunOnAssembly_whilePipelineRunsOnSubscription() {
		var assemblyCalls = new AtomicInteger();

		Mono<String> mono = findUser(assemblyCalls);

		assertThat(assemblyCalls).hasValue(1);
		assertThat(calls).hasValue(0);
		mono.block();
		assertThat(calls).hasValue(1);
	}

	// Cold Publisher：每次訂閱都從頭執行一次。同一個 WebClient 的 Mono 訂閱兩次，就會發出兩次 HTTP 請求
	@Test
	void mono_shouldExecuteAgain_whenSubscribedTwice() {
		Mono<String> mono = Mono.fromCallable(this::expensiveQuery);

		mono.block();
		mono.block();

		assertThat(calls).hasValue(2);
	}

	// cache()：第一次訂閱的結果保存下來，之後的訂閱直接取得結果
	@Test
	void cache_shouldExecuteOnce_whenSubscribedTwice() {
		Mono<String> mono = Mono.fromCallable(this::expensiveQuery).cache();

		mono.block();
		mono.block();

		assertThat(calls).hasValue(1);
	}
}
