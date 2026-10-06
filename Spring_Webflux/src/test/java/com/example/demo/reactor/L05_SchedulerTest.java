package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * 第 5 課：程式跑在哪個執行緒？
 *
 * <p>
 * 直覺以為：一個請求一個執行緒，從頭到尾都在同一個執行緒上。<br>
 * 實際上：Reactor 本身不會開執行緒，每個操作子跑在「把資料送過來的那個執行緒」上；只有遇到 delay、publishOn、subscribeOn
 * 或非同步的 I/O（例如 WebClient、R2DBC）才會換執行緒。
 *
 * <pre>
 * Scheduler                      用途                                執行緒名稱
 * Schedulers.parallel()          CPU 運算、delay / interval 的計時    parallel-N（數量 = CPU 核心數，不能阻塞）
 * Schedulers.boundedElastic()    包裝阻塞呼叫（JDBC、檔案、舊的 SDK）  boundedElastic-N（數量有上限）
 * </pre>
 */
class L05_SchedulerTest {

	private final List<String> threads = new CopyOnWriteArrayList<>();

	private <T> T recordThread(String step, T value) {
		threads.add(step + "@" + Thread.currentThread().getName());
		return value;
	}

	// 沒有任何非同步的操作子：全部在呼叫 subscribe（這裡是 block）的執行緒上執行
	@Test
	void pipeline_shouldRunOnSubscribingThread_whenNothingIsAsync() {
		String caller = Thread.currentThread().getName();

		Mono.just(1).map(i -> recordThread("map", i)).block();

		assertThat(threads).containsExactly("map@" + caller);
	}

	// delayElement 在 parallel 執行緒上計時，時間到了就在那個執行緒上繼續往下送
	@Test
	void delay_shouldSwitchToParallelThread() {
		Mono.just(1)
				.map(i -> recordThread("before", i))
				.delayElement(Duration.ofMillis(10))
				.map(i -> recordThread("after", i))
				.block();

		assertThat(threads.get(0)).doesNotContain("parallel");
		assertThat(threads.get(1)).startsWith("after@parallel-");
	}

	// publishOn：影響它「下面」的操作子
	@Test
	void publishOn_shouldAffectDownstreamOperators() {
		Mono.just(1)
				.map(i -> recordThread("above", i))
				.publishOn(Schedulers.boundedElastic())
				.map(i -> recordThread("below", i))
				.block();

		assertThat(threads.get(0)).doesNotContain("boundedElastic");
		assertThat(threads.get(1)).startsWith("below@boundedElastic-");
	}

	// subscribeOn：影響「訂閱」與資料來源，不論寫在 chain 的哪個位置
	@Test
	void subscribeOn_shouldAffectSourceRegardlessOfPosition() {
		Mono.fromCallable(() -> recordThread("source", 1))
				.map(i -> recordThread("map", i))
				.subscribeOn(Schedulers.boundedElastic()) // 寫在最後面，仍然影響最上面的 fromCallable
				.block();

		assertThat(threads).allMatch(t -> t.contains("@boundedElastic-"));
	}

	// 在不允許阻塞的執行緒（parallel、Netty 的 event loop）上呼叫 block()，Reactor 會直接拋出例外。
	// 只有「真的需要等待」才會檢查：Mono.just(1).block() 已經有結果、不必等待，所以不會拋出例外
	@Test
	void block_shouldFail_whenCalledOnNonBlockingThread() {
		Mono<Long> mono = Mono.delay(Duration.ofMillis(10)) // 之後在 parallel 執行緒上
				.map(tick -> Mono.delay(Duration.ofMillis(10)).block());

		StepVerifier.create(mono)
				.expectErrorSatisfies(e -> assertThat(e)
						.isInstanceOf(IllegalStateException.class)
						.hasMessageContaining("block()/blockFirst()/blockLast() are blocking, which is not supported in thread parallel-"))
				.verify();
	}

	/** 模擬阻塞的舊 API（例如 JDBC、同步的 SDK） */
	private String legacyBlockingCall() {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return recordThread("blocking", "結果");
	}

	// 阻塞呼叫的正確包裝方式：fromCallable 延後執行 + subscribeOn(boundedElastic) 交給專門給阻塞用的執行緒
	@Test
	void blockingCall_shouldRunOnBoundedElastic_whenWrappedProperly() {
		Mono<String> mono = Mono.fromCallable(this::legacyBlockingCall).subscribeOn(Schedulers.boundedElastic());

		StepVerifier.create(mono).expectNext("結果").verifyComplete();
		assertThat(threads).singleElement().asString().startsWith("blocking@boundedElastic-");
	}
}
