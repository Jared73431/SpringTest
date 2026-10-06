package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 第 8 課：自己產生資料 —— generate 與 create。
 *
 * <p>
 * 直覺以為：要把資料變成 Flux，就把它們放進 List 再 Flux.fromIterable。<br>
 * 實際上：資料常常不是一開始就準備好的：要一筆一筆計算（generate），或是由舊的 API 用 callback / listener 推過來（create）。
 *
 * <pre>
 *                generate                        create
 * 誰決定何時產生   下游 request 一筆，才呼叫一次     來源自己推（listener、callback），不管下游
 * 每次可送幾筆     最多 1 筆                       任意多筆，可以從其他執行緒送
 * 背壓            天生支援（拉）                   不支援，要選 OverflowStrategy（第 7 課）
 * 適用            依狀態逐筆計算的序列               把 callback / listener 形式的舊 API 包成 Flux
 * </pre>
 */
class L08_CreateTest {

	// ---- generate ----

	// 狀態（state）從 1 開始，每次呼叫產生一筆並回傳下一個狀態
	@Test
	void generate_shouldProduceItemsFromState() {
		Flux<String> table = Flux.generate(() -> 1, (state, sink) -> {
			sink.next("3 x " + state + " = " + 3 * state);
			if (state == 3) {
				sink.complete();
			}
			return state + 1;
		});

		StepVerifier.create(table).expectNext("3 x 1 = 3", "3 x 2 = 6", "3 x 3 = 9").verifyComplete();
	}

	// generate 是「拉」的：下游要幾筆，產生器就只被呼叫幾次
	@Test
	void generate_shouldCallGeneratorOnlyAsManyTimesAsRequested() {
		var calls = new AtomicInteger();
		Flux<Integer> naturals = Flux.generate(() -> 1, (state, sink) -> {
			calls.incrementAndGet();
			sink.next(state);
			return state + 1;
		});

		StepVerifier.create(naturals, 2)
				.expectNext(1, 2)
				.then(() -> assertThat(calls).hasValue(2)) // 無限序列，但只算了 2 筆
				.thenCancel()
				.verify();
	}

	@Test
	void generate_shouldFail_whenNextIsCalledTwiceInOneRound() {
		Flux<Integer> invalid = Flux.generate(sink -> {
			sink.next(1);
			sink.next(2); // 每一輪最多只能送一筆
		});

		StepVerifier.create(invalid)
				.expectNext(1)
				.expectErrorMatches(e -> e instanceof IllegalStateException
						&& e.getMessage().contains("More than one call to onNext"))
				.verify();
	}

	// ---- create：包裝 listener 形式的舊 API ----

	/** 模擬 listener 形式的舊 API（例如 SDK 的事件通知、訊息佇列的 consumer）：價格變動時呼叫所有 listener */
	static class PriceTicker {

		private final List<Consumer<Double>> listeners = new CopyOnWriteArrayList<>();

		void addListener(Consumer<Double> listener) {
			listeners.add(listener);
		}

		void removeListener(Consumer<Double> listener) {
			listeners.remove(listener);
		}

		void publish(double price) {
			listeners.forEach(listener -> listener.accept(price));
		}

		int listenerCount() {
			return listeners.size();
		}
	}

	private static Flux<Double> prices(PriceTicker ticker) {
		return Flux.create(sink -> {
			Consumer<Double> listener = sink::next; // listener 收到資料就推給 Flux
			ticker.addListener(listener); // 訂閱時才註冊（第 1 課：沒有訂閱就什麼都不做）
			sink.onDispose(() -> ticker.removeListener(listener)); // 取消或完成時取消註冊，避免 listener 洩漏
		});
	}

	@Test
	void create_shouldBridgeListenerApi_andUnregisterOnCancel() {
		var ticker = new PriceTicker();
		var received = new CopyOnWriteArrayList<Double>();

		var subscription = prices(ticker).subscribe(received::add);
		assertThat(ticker.listenerCount()).isEqualTo(1);

		ticker.publish(100.0);
		ticker.publish(101.5);
		subscription.dispose();
		ticker.publish(99.0); // 已經取消，收不到

		assertThat(received).containsExactly(100.0, 101.5);
		assertThat(ticker.listenerCount()).isZero();
	}

	// ---- create 不理會背壓：下游只要 1 筆，來源卻一次推 3 筆，要選擇怎麼處理 ----

	private static Flux<Integer> pushThree(FluxSink.OverflowStrategy strategy) {
		return Flux.create(sink -> {
			sink.next(1);
			sink.next(2);
			sink.next(3);
			sink.complete();
		}, strategy);
	}

	// BUFFER（預設）：先存起來，下游要了再給
	@Test
	void create_shouldBufferExtraItems_whenStrategyIsBuffer() {
		StepVerifier.create(pushThree(FluxSink.OverflowStrategy.BUFFER), 1)
				.expectNext(1)
				.thenRequest(2).expectNext(2, 3)
				.verifyComplete();
	}

	// DROP：下游沒有要的就丟掉
	@Test
	void create_shouldDropExtraItems_whenStrategyIsDrop() {
		StepVerifier.create(pushThree(FluxSink.OverflowStrategy.DROP), 1)
				.expectNext(1)
				.verifyComplete();
	}

	// ERROR：直接以 OverflowException 結束
	@Test
	void create_shouldFail_whenStrategyIsErrorAndDownstreamIsSlow() {
		StepVerifier.create(pushThree(FluxSink.OverflowStrategy.ERROR), 1)
				.expectNext(1)
				.expectErrorMatches(Exceptions::isOverflow)
				.verify();
	}

	// ---- Mono.create：包裝只回呼一次的 callback API ----

	/** 模擬 callback 形式的非同步 API（例如 OkHttp 的 enqueue、舊版 SDK） */
	interface Callback {
		void onSuccess(String body);

		void onFailure(Exception e);
	}

	private static void legacyAsyncCall(String id, Callback callback) {
		if (id.isBlank()) {
			callback.onFailure(new IllegalArgumentException("id 不可空白"));
		} else {
			callback.onSuccess("資料 " + id);
		}
	}

	private static Mono<String> fetch(String id) {
		return Mono.create(sink -> legacyAsyncCall(id, new Callback() {
			@Override
			public void onSuccess(String body) {
				sink.success(body);
			}

			@Override
			public void onFailure(Exception e) {
				sink.error(e); // callback 的失敗變成 onError 訊號（第 5 課）
			}
		}));
	}

	// 回傳 CompletableFuture 的 API 不必自己包：用 Mono.fromFuture(() -> api.call()) 即可
	@Test
	void monoCreate_shouldBridgeCallbackApi() {
		StepVerifier.create(fetch("42")).expectNext("資料 42").verifyComplete();
		StepVerifier.create(fetch(" ")).expectErrorMessage("id 不可空白").verify();
	}
}
