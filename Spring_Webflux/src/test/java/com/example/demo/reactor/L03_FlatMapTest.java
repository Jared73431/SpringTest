package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 第 3 課：map / flatMap / concatMap / flatMapSequential。
 *
 * <p>
 * 直覺以為：flatMap 是「回傳 Mono 時用的 map」，順序和原本一樣。<br>
 * 實際上：flatMap 會<b>同時</b>訂閱所有內部的 Mono，誰先完成誰先出來，順序不保證。
 *
 * <pre>
 * 操作子              同時執行                保持順序
 * flatMap             是                      否
 * concatMap           否（一個做完才做下一個）  是
 * flatMapSequential   是                      是（先完成的會等前面的）
 * </pre>
 *
 * 用 withVirtualTime 控制時間：延遲幾百毫秒的測試不必真的等待，而且結果是確定的。
 */
class L03_FlatMapTest {

	/** 模擬查詢：id 是幾，就花 id * 100ms */
	private static Mono<Integer> slowLookup(int id) {
		return Mono.just(id).delayElement(Duration.ofMillis(id * 100L));
	}

	// map 的 lambda 回傳 Mono 時，得到的是「一串 Mono 物件」，而且裡面的查詢都沒有被訂閱
	@Test
	void map_shouldEmitUnsubscribedMonos_whenFunctionReturnsMono() {
		var calls = new AtomicInteger();

		Flux<Mono<Integer>> result = Flux.just(1, 2, 3)
				.map(id -> Mono.fromCallable(calls::incrementAndGet));

		StepVerifier.create(result).expectNextCount(3).verifyComplete();
		assertThat(calls).hasValue(0);
	}

	@Test
	void flatMap_shouldEmitInCompletionOrder() {
		StepVerifier.withVirtualTime(() -> Flux.just(3, 1, 2).flatMap(L03_FlatMapTest::slowLookup))
				.thenAwait(Duration.ofMillis(100)).expectNext(1)
				.thenAwait(Duration.ofMillis(100)).expectNext(2)
				.thenAwait(Duration.ofMillis(100)).expectNext(3)
				.verifyComplete(); // 全部同時執行，總共只花 300ms
	}

	@Test
	void concatMap_shouldKeepOrderButRunOneAtATime() {
		StepVerifier.withVirtualTime(() -> Flux.just(3, 1, 2).concatMap(L03_FlatMapTest::slowLookup))
				.thenAwait(Duration.ofMillis(300)).expectNext(3)
				.thenAwait(Duration.ofMillis(100)).expectNext(1)
				.thenAwait(Duration.ofMillis(200)).expectNext(2)
				.verifyComplete(); // 一個接一個：300 + 100 + 200 = 600ms
	}

	@Test
	void flatMapSequential_shouldRunConcurrentlyAndKeepOrder() {
		StepVerifier.withVirtualTime(() -> Flux.just(3, 1, 2).flatMapSequential(L03_FlatMapTest::slowLookup))
				.expectSubscription()
				.expectNoEvent(Duration.ofMillis(299)) // 1、2 已經完成，但要等 3，所以什麼都還沒送出
				.thenAwait(Duration.ofMillis(1)).expectNext(3, 1, 2)
				.verifyComplete(); // 同時執行，總共 300ms
	}

	// flatMap 的第二個參數限制同時執行的數量；設為 1 就等同 concatMap
	@Test
	void flatMap_shouldKeepOrder_whenConcurrencyIsOne() {
		StepVerifier.withVirtualTime(() -> Flux.just(3, 1, 2).flatMap(L03_FlatMapTest::slowLookup, 1))
				.thenAwait(Duration.ofMillis(600)).expectNext(3, 1, 2)
				.verifyComplete();
	}

	// zip：同時執行兩個互不相關的查詢，等兩個都完成再合併。總時間是較慢的那一個，不是兩者相加
	@Test
	void zip_shouldRunBothConcurrently() {
		StepVerifier.withVirtualTime(() -> Mono.zip(slowLookup(3), slowLookup(2), Integer::sum))
				.thenAwait(Duration.ofMillis(300)).expectNext(5)
				.verifyComplete();
	}
}
