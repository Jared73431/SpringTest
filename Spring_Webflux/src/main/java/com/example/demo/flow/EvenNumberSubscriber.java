package com.example.demo.flow;

/**
 * 自訂 Subscriber：只保留偶數。奇數同樣會被接收（也同樣要 request 下一筆），只是不記錄。
 */
public class EvenNumberSubscriber extends RecordingSubscriber<Integer> {

	@Override
	protected void handle(Integer item) {
		if (item % 2 == 0) {
			super.handle(item);
		}
	}
}
