package com.example.demo.flow;

/**
 * 收到 "error" 時拋出例外的 Subscriber。
 *
 * <p>
 * SubmissionPublisher 的行為：onNext 拋出例外 → 取消這個訂閱並呼叫 onError，之後的資料不會再送達，也不會呼叫 onComplete。
 */
public class ErrorProneSubscriber extends RecordingSubscriber<String> {

	@Override
	protected void handle(String item) {
		if ("error".equals(item)) {
			throw new IllegalStateException("處理 " + item + " 時發生錯誤");
		}
		super.handle(item);
	}
}
