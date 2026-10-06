package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * 第 6 課：背壓（第 0 課的 request(n)，在 Reactor 中的樣子）。
 *
 * <p>
 * 直覺以為：subscribe() 會一筆一筆慢慢要。<br>
 * 實際上：直接 subscribe() 會 request(Long.MAX_VALUE)，等於「全部給我」。只有 Subscriber 真的要求少一點（或中間用 limitRate），
 * 背壓才會發生作用。
 *
 * <p>
 * 有些來源無法放慢，例如計時器（interval）、使用者事件、即時報價。下游跟不上時必須選擇策略：
 *
 * <pre>
 * 預設                    來不及送就拋出 OverflowException
 * onBackpressureDrop      丟掉下游來不及要的資料
 * onBackpressureLatest    只保留最新的一筆
 * onBackpressureBuffer    先存起來（要設上限，否則可能耗盡記憶體）
 * </pre>
 */
class L06_BackpressureTest {

	private final List<Long> requests = new CopyOnWriteArrayList<>();

	@Test
	void subscribe_shouldRequestUnbounded_byDefault() {
		Flux.range(1, 100).doOnRequest(requests::add).blockLast();

		assertThat(requests).containsExactly(Long.MAX_VALUE);
	}

	// limitRate：分批向上游要，每批最多 10 筆（消費到 75% 時補要下一批）
	@Test
	void limitRate_shouldRequestInBatches() {
		Flux.range(1, 100).doOnRequest(requests::add).limitRate(10).blockLast();

		assertThat(requests.getFirst()).isEqualTo(10);
		assertThat(requests).allMatch(n -> n <= 10);
	}

	// StepVerifier 也可以扮演「一開始什麼都不要」的 Subscriber，再一次要幾筆
	@Test
	void subscriber_shouldControlHowManyItemsItReceives() {
		StepVerifier.create(Flux.range(1, 5), 0)
				.expectSubscription()
				.expectNoEvent(Duration.ofMillis(50)) // 還沒 request，什麼都不會送
				.thenRequest(2).expectNext(1, 2)
				.thenRequest(3).expectNext(3, 4, 5)
				.verifyComplete();
	}

	// interval 是計時器，無法放慢；下游沒有要求時時間一到就沒地方送，於是拋出 OverflowException
	@Test
	void interval_shouldFailWithOverflow_whenDownstreamDoesNotRequest() {
		StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofSeconds(1)), 0)
				.expectSubscription()
				.thenAwait(Duration.ofSeconds(1))
				.expectErrorMatches(Exceptions::isOverflow)
				.verify();
	}

	@Test
	void onBackpressureDrop_shouldDiscardItemsNotRequested() {
		var dropped = new CopyOnWriteArrayList<Long>();

		StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofSeconds(1)).onBackpressureDrop(dropped::add), 0)
				.expectSubscription()
				.thenAwait(Duration.ofSeconds(5)) // 0~4 秒的 5 筆，下游都沒有要求
				.thenRequest(1)
				.thenAwait(Duration.ofSeconds(1)).expectNext(5L) // 要求之後才收到下一筆
				.thenCancel()
				.verify();

		assertThat(dropped).containsExactly(0L, 1L, 2L, 3L, 4L);
	}

	@Test
	void onBackpressureLatest_shouldKeepOnlyTheNewestItem() {
		StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofSeconds(1)).onBackpressureLatest(), 0)
				.expectSubscription()
				.thenAwait(Duration.ofSeconds(5))
				.thenRequest(1).expectNext(4L) // 立刻拿到目前最新的一筆
				.thenCancel()
				.verify();
	}

	// Virtual time：每分鐘一筆、持續一小時的串流，測試瞬間完成
	@Test
	void virtualTime_shouldVerifyOneHourStreamInstantly() {
		StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofMinutes(1)).take(60))
				.thenAwait(Duration.ofHours(1))
				.expectNextCount(60)
				.verifyComplete();
	}
}
