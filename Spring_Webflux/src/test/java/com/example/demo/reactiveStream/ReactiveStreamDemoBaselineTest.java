package com.example.demo.reactiveStream;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Baseline：鎖定重構前 ReactiveStreamDemo 各範例印出的內容（範例只會印到 console，所以攔截 System.out / System.err）。
 * 背壓範例固定等待 10 秒且輸出與下一個範例交錯，不納入 baseline。
 */
class ReactiveStreamDemoBaselineTest {

	private final PrintStream originalOut = System.out;
	private final PrintStream originalErr = System.err;
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private final ByteArrayOutputStream err = new ByteArrayOutputStream();

	@BeforeEach
	void captureConsole() {
		System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
		System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void restoreConsole() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	private List<String> outLines() {
		return out.toString(StandardCharsets.UTF_8).lines().toList();
	}

	private List<String> errLines() {
		return err.toString(StandardCharsets.UTF_8).lines().toList();
	}

	@Test
	void basicPublisherSubscriber_shouldReceiveAllItemsThenComplete() {
		ReactiveStreamDemo.basicPublisherSubscriber();

		assertThat(outLines()).containsExactly("訂閱成功！", "接收到: Hello", "接收到: Reactive", "接收到: Stream", "數據流完成！");
	}

	@Test
	void customSubscriber_shouldClassifyOddAndEvenNumbers() {
		ReactiveStreamDemo.customSubscriber();

		assertThat(outLines()).containsExactly("EvenNumberSubscriber 訂閱成功", "跳過奇數: 1", "處理偶數: 2", "跳過奇數: 3",
				"處理偶數: 4", "跳過奇數: 5", "處理偶數: 6", "跳過奇數: 7", "處理偶數: 8", "跳過奇數: 9", "處理偶數: 10",
				"EvenNumberSubscriber 完成");
	}

	@Test
	void processorExample_shouldUppercaseItems() {
		ReactiveStreamDemo.processorExample();

		assertThat(outLines()).containsExactly("處理後 訂閱成功", "處理後 接收: HELLO", "處理後 接收: WORLD", "處理後 接收: REACTIVE",
				"處理後 完成");
	}

	// onNext 拋出例外 → SubmissionPublisher 取消訂閱並呼叫 onError，之後的資料不會再送達，也不會呼叫 onComplete
	@Test
	void errorHandlingExample_shouldStopAndCallOnError_whenOnNextThrows() {
		ReactiveStreamDemo.errorHandlingExample();

		assertThat(outLines()).containsExactly("ErrorProneSubscriber 訂閱成功", "ErrorProneSubscriber 處理: 正常數據");
		assertThat(errLines()).containsExactly("ErrorProneSubscriber 錯誤: 處理 error 時發生錯誤");
	}
}
