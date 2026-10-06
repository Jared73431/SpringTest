package com.example.demo.flow;

import java.time.Duration;

/**
 * 每筆資料都要處理一段時間的 Subscriber，用來觀察背壓：消費得慢，生產者就會被迫放慢。
 */
public class SlowSubscriber<T> extends RecordingSubscriber<T> {

	private final Duration delay;

	public SlowSubscriber(Duration delay) {
		this.delay = delay;
	}

	@Override
	protected void handle(T item) {
		try {
			Thread.sleep(delay); // 模擬慢速處理
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		super.handle(item);
	}
}
