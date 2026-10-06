package com.example.demo.flow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

/**
 * 最基本的 Subscriber：一次要一筆（request(1)），處理完再要下一筆。
 *
 * <p>
 * 收到的資料記錄在 {@link #received()}；{@link #completion()} 在 onComplete 時正常結束、在 onError 時以該錯誤結束，
 * 呼叫端可以等待它，不必用 Thread.sleep 猜要等多久。
 *
 * <p>
 * 子類別覆寫 {@link #handle(Object)} 決定如何處理每一筆資料。
 */
public class RecordingSubscriber<T> implements Flow.Subscriber<T> {

	// onNext 在 Publisher 的執行緒呼叫，received() 在呼叫端的執行緒讀取，所以使用執行緒安全的 List
	private final List<T> received = new CopyOnWriteArrayList<>();
	private final CompletableFuture<Void> completion = new CompletableFuture<>();
	private Flow.Subscription subscription;

	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		this.subscription = subscription;
		subscription.request(1); // 沒有 request，Publisher 一筆都不會送
	}

	@Override
	public void onNext(T item) {
		handle(item);
		subscription.request(1); // 處理完才要下一筆：Subscriber 控制速度，這就是背壓
	}

	@Override
	public void onError(Throwable throwable) {
		completion.completeExceptionally(throwable);
	}

	@Override
	public void onComplete() {
		completion.complete(null);
	}

	protected void handle(T item) {
		received.add(item);
	}

	public List<T> received() {
		return List.copyOf(received);
	}

	public CompletableFuture<Void> completion() {
		return completion;
	}
}
