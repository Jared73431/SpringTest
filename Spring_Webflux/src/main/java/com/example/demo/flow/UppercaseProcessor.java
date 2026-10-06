package com.example.demo.flow;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Processor = 同時是 Subscriber（接收上游）也是 Publisher（送給下游），這裡把字串轉成大寫。
 *
 * <p>
 * 繼承 SubmissionPublisher 取得「送給下游」的能力，自己實作「接收上游」的部分。
 * Reactor 的 map、filter 等操作子，概念上就是一個個 Processor 串起來。
 */
public class UppercaseProcessor extends SubmissionPublisher<String> implements Flow.Processor<String, String> {

	private Flow.Subscription subscription;

	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		this.subscription = subscription;
		subscription.request(1);
	}

	@Override
	public void onNext(String item) {
		submit(item.toUpperCase()); // 送給下游；下游的緩衝區滿了會在這裡阻塞，背壓就這樣一路傳回上游
		subscription.request(1);
	}

	@Override
	public void onError(Throwable throwable) {
		closeExceptionally(throwable); // 把錯誤傳給下游
	}

	@Override
	public void onComplete() {
		close(); // 把完成傳給下游
	}
}
