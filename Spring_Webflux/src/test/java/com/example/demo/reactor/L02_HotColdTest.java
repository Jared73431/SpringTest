package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.scheduler.VirtualTimeScheduler;

/**
 * 第 2 課：Cold 與 Hot。
 *
 * <p>
 * 直覺以為：每個訂閱者都會拿到完整的資料。<br>
 * 實際上：要看資料來源是哪一種。
 *
 * <pre>
 * Cold（第 1 課）  每個訂閱者各自從頭執行一次，大家都拿到完整的資料（例如 HTTP 呼叫、資料庫查詢、Flux.range）
 * Hot             資料來源只有一個，不管有沒有人訂閱都在產生；晚來的人只會收到「訂閱之後」的資料
 *                 （例如股價、聊天訊息、使用者點擊）
 * </pre>
 *
 * 時間用 VirtualTimeScheduler 手動推進：兩個訂閱者在不同時間加入，比較各自收到什麼。
 */
class L02_HotColdTest {

	private final VirtualTimeScheduler clock = VirtualTimeScheduler.create();
	private final List<Long> first = new CopyOnWriteArrayList<>();
	private final List<Long> second = new CopyOnWriteArrayList<>();

	/** 每秒一筆：0、1、2… */
	private Flux<Long> ticks() {
		return Flux.interval(Duration.ofSeconds(1), clock);
	}

	private void advanceSeconds(long seconds) {
		clock.advanceTimeBy(Duration.ofSeconds(seconds));
	}

	// ---- Cold ----

	@Test
	void coldFlux_shouldStartFromBeginningForEachSubscriber() {
		Flux<Long> cold = ticks();

		cold.subscribe(first::add);
		advanceSeconds(2);
		cold.subscribe(second::add); // 晚 2 秒才訂閱
		advanceSeconds(2);

		assertThat(first).containsExactly(0L, 1L, 2L, 3L);
		assertThat(second).containsExactly(0L, 1L); // 自己的計時器，從 0 開始
	}

	// ---- 把 Cold 變成 Hot ----

	// share()：所有訂閱者共用同一個來源，晚來的人從「現在」開始收
	@Test
	void share_shouldMakeLateSubscriberMissEarlierItems() {
		Flux<Long> hot = ticks().share();

		hot.subscribe(first::add);
		advanceSeconds(2);
		hot.subscribe(second::add);
		advanceSeconds(2);

		assertThat(first).containsExactly(0L, 1L, 2L, 3L);
		assertThat(second).containsExactly(2L, 3L); // 0、1 已經錯過了
	}

	// publish() 得到 ConnectableFlux：訂閱只是「登記」，要等 connect() 才開始送資料
	@Test
	void connectableFlux_shouldNotStartUntilConnect() {
		var sourceSubscriptions = new AtomicInteger();
		ConnectableFlux<Integer> connectable = Flux.range(1, 3)
				.doOnSubscribe(s -> sourceSubscriptions.incrementAndGet())
				.publish();
		var a = new CopyOnWriteArrayList<Integer>();
		var b = new CopyOnWriteArrayList<Integer>();

		connectable.subscribe(a::add);
		connectable.subscribe(b::add);
		assertThat(a).isEmpty(); // 還沒 connect
		assertThat(sourceSubscriptions).hasValue(0);

		connectable.connect();

		assertThat(a).containsExactly(1, 2, 3);
		assertThat(b).containsExactly(1, 2, 3);
		assertThat(sourceSubscriptions).hasValue(1); // 兩個訂閱者，來源只執行一次
	}

	// autoConnect(n)：等到第 n 個訂閱者才自動 connect
	@Test
	void autoConnect_shouldStartWhenEnoughSubscribersArrive() {
		Flux<Long> hot = ticks().publish().autoConnect(2);

		hot.subscribe(first::add);
		advanceSeconds(2);
		assertThat(first).isEmpty(); // 只有 1 個訂閱者，還沒開始

		hot.subscribe(second::add);
		advanceSeconds(2);

		assertThat(first).containsExactly(0L, 1L);
		assertThat(second).containsExactly(0L, 1L);
	}

	// refCount：最後一個訂閱者取消時，也取消上游（share() 就是 publish().refCount() 的簡寫）
	@Test
	void refCount_shouldCancelSourceWhenLastSubscriberLeaves() {
		var sourceCancelled = new AtomicBoolean();
		Flux<Long> hot = ticks().doOnCancel(() -> sourceCancelled.set(true)).publish().refCount();

		Disposable a = hot.subscribe(first::add);
		Disposable b = hot.subscribe(second::add);
		advanceSeconds(1);

		a.dispose();
		assertThat(sourceCancelled).isFalse(); // 還有 1 個訂閱者
		b.dispose();
		assertThat(sourceCancelled).isTrue(); // 沒有人了，停止上游（例如關閉 WebSocket 連線）
	}

	// replay(n)：保留最近 n 筆，晚來的人先收到這幾筆，再接著收即時的資料
	@Test
	void replay_shouldGiveLateSubscriberRecentItems() {
		Flux<Long> hot = ticks().replay(1).autoConnect();

		hot.subscribe(first::add);
		advanceSeconds(2);
		hot.subscribe(second::add);
		advanceSeconds(2);

		assertThat(first).containsExactly(0L, 1L, 2L, 3L);
		assertThat(second).containsExactly(1L, 2L, 3L); // 先收到最近的 1，再收即時的 2、3
	}

	// ---- Mono.just 的「Hot」和 Sinks 的 Hot 不一樣 ----

	// Reactor 文件把 just 歸類為 Hot：值在組裝時就決定了。但每個訂閱者都拿得到同一個值，不會漏掉
	@Test
	void just_shouldGiveSameCapturedValueToEverySubscriber() {
		var counter = new AtomicInteger();
		Mono<Integer> just = Mono.just(counter.incrementAndGet());
		Mono<Integer> deferred = Mono.defer(() -> Mono.just(counter.incrementAndGet()));

		assertThat(List.of(just.block(), just.block())).containsExactly(1, 1);
		assertThat(List.of(deferred.block(), deferred.block())).containsExactly(2, 3); // Cold：每次重新計算
	}

	// ---- Sinks：由程式主動推送資料的 Hot 來源 ----

	// multicast：多個訂閱者共用；沒有人訂閱時送出的資料直接丟掉（directBestEffort）
	@Test
	void multicastSink_shouldDeliverOnlyItemsEmittedAfterSubscribing() {
		Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();
		var a = new CopyOnWriteArrayList<String>();
		var b = new CopyOnWriteArrayList<String>();

		assertThat(sink.tryEmitNext("沒人聽")).isEqualTo(Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER);
		sink.asFlux().subscribe(a::add);
		sink.tryEmitNext("第一則");
		sink.asFlux().subscribe(b::add);
		sink.tryEmitNext("第二則");

		assertThat(a).containsExactly("第一則", "第二則");
		assertThat(b).containsExactly("第二則");
	}

	// replay：保留歷史資料，晚來的人也看得到（例如聊天室的最近 N 則訊息：replay().limit(N)）
	@Test
	void replaySink_shouldDeliverHistoryToLateSubscriber() {
		Sinks.Many<String> sink = Sinks.many().replay().limit(2);
		var late = new CopyOnWriteArrayList<String>();

		sink.tryEmitNext("1");
		sink.tryEmitNext("2");
		sink.tryEmitNext("3");
		sink.asFlux().subscribe(late::add);

		assertThat(late).containsExactly("2", "3");
	}

	// unicast：只允許一個訂閱者
	@Test
	void unicastSink_shouldRejectSecondSubscriber() {
		Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
		var error = new CopyOnWriteArrayList<Throwable>();

		sink.asFlux().subscribe();
		sink.asFlux().subscribe(item -> {
		}, error::add);

		assertThat(error).singleElement().satisfies(e -> assertThat(e)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("only allow a single Subscriber"));
	}
}
