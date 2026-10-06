package com.example.demo.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * 第 0 課：Reactive Streams 規範（JDK 9 的 java.util.concurrent.Flow）。
 *
 * <p>
 * Reactor（Mono / Flux）與 WebFlux 都建立在這 4 個介面上：Publisher、Subscriber、Subscription、Processor。
 * 最重要的一點：<b>資料不是 Publisher 想送就送，而是 Subscriber 用 request(n) 要多少才送多少</b>。
 *
 * <p>
 * SubmissionPublisher 在其他執行緒把資料送給 Subscriber，所以測試等待 completion()，而不是 Thread.sleep。
 */
class L00_FlowApiTest {

	private static final long TIMEOUT_SECONDS = 5;

	private static void await(RecordingSubscriber<?> subscriber) throws Exception {
		subscriber.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	@Test
	void subscriber_shouldReceiveAllItemsThenComplete_whenPublisherCloses() throws Exception {
		var subscriber = new RecordingSubscriber<String>();
		try (var publisher = new SubmissionPublisher<String>()) {
			publisher.subscribe(subscriber);
			publisher.submit("Hello");
			publisher.submit("Reactive");
			publisher.submit("Stream");
		} // close()：送完已提交的資料後呼叫 onComplete

		await(subscriber);
		assertThat(subscriber.received()).containsExactly("Hello", "Reactive", "Stream");
	}

	// 沒有 request，資料就只會留在 Publisher 的緩衝區，一筆都不會送到 onNext
	@Test
	void subscriber_shouldReceiveNothing_whenItNeverRequests() throws Exception {
		var subscribed = new CountDownLatch(1);
		var received = new AtomicInteger();
		Flow.Subscriber<String> lazySubscriber = new Flow.Subscriber<>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscribed.countDown(); // 刻意不呼叫 subscription.request(n)
			}

			@Override
			public void onNext(String item) {
				received.incrementAndGet();
			}

			@Override
			public void onError(Throwable throwable) {
			}

			@Override
			public void onComplete() {
			}
		};

		try (var publisher = new SubmissionPublisher<String>()) {
			publisher.subscribe(lazySubscriber);
			assertThat(subscribed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

			// submit 回傳「已送出但尚未被消費」的估計數量：沒有人消費，所以一直增加
			assertThat(List.of(publisher.submit("a"), publisher.submit("b"), publisher.submit("c")))
					.containsExactly(1, 2, 3);
			assertThat(publisher.estimateMaximumLag()).isEqualTo(3);
			assertThat(received).hasValue(0);
		}
	}

	@Test
	void evenNumberSubscriber_shouldKeepOnlyEvenNumbers() throws Exception {
		var subscriber = new EvenNumberSubscriber();
		try (var publisher = new SubmissionPublisher<Integer>()) {
			publisher.subscribe(subscriber);
			for (int i = 1; i <= 10; i++) {
				publisher.submit(i);
			}
		}

		await(subscriber);
		assertThat(subscriber.received()).containsExactly(2, 4, 6, 8, 10);
	}

	// Publisher → Processor → Subscriber，完成訊號也會經由 Processor 傳到下游
	//
	// 注意：processor 不能放進 try-with-resources。資源以相反順序關閉，processor 會比 publisher 先 close，
	// 上游的資料還沒送到，下游就先收到 onComplete（結果一筆都收不到）。processor 要等上游的 onComplete 再自己 close。
	@Test
	void processor_shouldTransformItemsAndPassCompletionDownstream() throws Exception {
		var subscriber = new RecordingSubscriber<String>();
		var processor = new UppercaseProcessor();
		try (var publisher = new SubmissionPublisher<String>()) {
			publisher.subscribe(processor);
			processor.subscribe(subscriber);
			publisher.submit("hello");
			publisher.submit("world");
			publisher.submit("reactive");
		}

		await(subscriber);
		assertThat(subscriber.received()).containsExactly("HELLO", "WORLD", "REACTIVE");
	}

	// onNext 拋出例外 → 訂閱被取消並呼叫 onError；之後的資料不會送達，也不會呼叫 onComplete
	@Test
	void subscriber_shouldStopAndReceiveOnError_whenOnNextThrows() {
		var subscriber = new ErrorProneSubscriber();
		try (var publisher = new SubmissionPublisher<String>()) {
			publisher.subscribe(subscriber);
			publisher.submit("正常資料");
			publisher.submit("error");
			publisher.submit("這筆不會被處理");
		}

		assertThatThrownBy(() -> await(subscriber)).isInstanceOf(ExecutionException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("處理 error 時發生錯誤");
		assertThat(subscriber.received()).containsExactly("正常資料");
	}

	@Test
	void batchSubscriber_shouldGroupItemsByRequestSize() throws Exception {
		var subscriber = new BatchSubscriber(3);
		try (var publisher = new SubmissionPublisher<Integer>()) {
			publisher.subscribe(subscriber);
			for (int i = 1; i <= 10; i++) {
				publisher.submit(i);
			}
		}

		subscriber.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		assertThat(subscriber.batches()).containsExactly(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7, 8, 9),
				List.of(10));
	}

	// ---- 背壓 ----

	// 修正前的註解寫「緩衝區大小為 5」，但實際容量會被調整成 2 的次方
	@Test
	void bufferCapacity_shouldBeRoundedUpToPowerOfTwo() {
		try (var publisher = new SubmissionPublisher<Integer>(ForkJoinPool.commonPool(), 5)) {
			assertThat(publisher.getMaxBufferCapacity()).isEqualTo(8);
		}
	}

	// submit：緩衝區滿了就阻塞，生產者被迫配合消費者的速度，資料一筆都不會遺失
	@Test
	void submit_shouldBlockProducer_whenBufferIsFull() throws Exception {
		var subscriber = new SlowSubscriber<Integer>(Duration.ofMillis(20));
		var lags = new ArrayList<Integer>();
		long start = System.nanoTime();
		int capacity;
		try (var publisher = new SubmissionPublisher<Integer>(ForkJoinPool.commonPool(), 4)) {
			capacity = publisher.getMaxBufferCapacity();
			publisher.subscribe(subscriber);
			for (int i = 1; i <= 20; i++) {
				lags.add(publisher.submit(i));
			}
		}
		Duration producerTime = Duration.ofNanos(System.nanoTime() - start);

		await(subscriber);
		assertThat(subscriber.received()).hasSize(20).isSorted();
		// 未消費的數量最多是緩衝區容量，加上 Subscriber 手上正在處理的 1 筆
		assertThat(lags).allMatch(lag -> lag <= capacity + 1);
		// 生產者本來可以瞬間送完 20 筆，卻因為阻塞而被拖慢到接近消費者的速度
		assertThat(producerTime).isGreaterThan(Duration.ofMillis(200));
	}

	// offer：緩衝區滿了不等待，直接丟棄（onDrop 回傳 false 表示不重試）。適合「最新的資料比較重要」的情況，例如即時報價
	@Test
	void offer_shouldDropItems_whenBufferIsFull() throws Exception {
		var subscriber = new SlowSubscriber<Integer>(Duration.ofMillis(50));
		var dropped = new AtomicInteger();
		try (var publisher = new SubmissionPublisher<Integer>(ForkJoinPool.commonPool(), 4)) {
			publisher.subscribe(subscriber);
			for (int i = 1; i <= 20; i++) {
				publisher.offer(i, (s, item) -> {
					dropped.incrementAndGet();
					return false;
				});
			}
		}

		await(subscriber);
		assertThat(dropped.get()).isPositive();
		assertThat(subscriber.received().size() + dropped.get()).isEqualTo(20);
	}
}
