package com.example.demo.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

/**
 * 批次 Subscriber：一次 request(batchSize) 筆，湊滿一批才處理並要下一批；完成時處理剩下不滿一批的資料。
 *
 * <p>
 * 與 {@link RecordingSubscriber} 一次要一筆對照：request(n) 的 n 決定了 Subscriber 一次願意接收多少。
 */
public class BatchSubscriber implements Flow.Subscriber<Integer> {

	private final int batchSize;
	private final List<Integer> current = new ArrayList<>();
	private final List<List<Integer>> batches = new CopyOnWriteArrayList<>();
	private final CompletableFuture<Void> completion = new CompletableFuture<>();
	private Flow.Subscription subscription;

	public BatchSubscriber(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		this.subscription = subscription;
		subscription.request(batchSize);
	}

	@Override
	public void onNext(Integer item) {
		current.add(item);
		if (current.size() >= batchSize) {
			flush();
			subscription.request(batchSize);
		}
	}

	@Override
	public void onError(Throwable throwable) {
		completion.completeExceptionally(throwable);
	}

	@Override
	public void onComplete() {
		if (!current.isEmpty()) {
			flush();
		}
		completion.complete(null);
	}

	private void flush() {
		batches.add(List.copyOf(current));
		current.clear();
	}

	public List<List<Integer>> batches() {
		return List.copyOf(batches);
	}

	public CompletableFuture<Void> completion() {
		return completion;
	}
}
