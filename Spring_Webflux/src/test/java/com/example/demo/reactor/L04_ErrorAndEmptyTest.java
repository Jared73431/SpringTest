package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 第 4 課：錯誤與空值都是「訊號」。
 *
 * <p>
 * 直覺以為：用 try/catch 接住錯誤；查不到資料就是 null。<br>
 * 實際上：錯誤是沿著 chain 往下傳的 onError 訊號，要用 onErrorXxx 操作子處理；查不到資料是 Mono.empty()（直接完成、沒有資料），
 * Reactor 不允許 null。
 */
class L04_ErrorAndEmptyTest {

	static class UserNotFoundException extends RuntimeException {
		UserNotFoundException(String id) {
			super("找不到使用者：" + id);
		}
	}

	// ---- 錯誤 ----

	@Test
	void tryCatch_shouldNotCatchError_becauseItHappensOnSubscription() {
		Mono<Integer> mono;
		try {
			mono = Mono.fromCallable(() -> divide(1, 0)); // 組裝時沒有錯誤
		} catch (ArithmeticException e) {
			mono = Mono.just(-1); // 永遠不會執行到
		}

		StepVerifier.create(mono).expectError(ArithmeticException.class).verify();
	}

	// 錯誤會終止整個串流：0 之後的 4 不會再處理
	@Test
	void error_shouldTerminateTheWholeFlux() {
		Flux<Integer> flux = Flux.just(1, 2, 0, 4).map(i -> divide(10, i));

		StepVerifier.create(flux).expectNext(10, 5).expectError(ArithmeticException.class).verify();
	}

	@Test
	void onErrorReturn_shouldReplaceErrorWithDefaultValue() {
		Flux<Integer> flux = Flux.just(1, 2, 0, 4).map(i -> divide(10, i)).onErrorReturn(-1);

		StepVerifier.create(flux).expectNext(10, 5, -1).verifyComplete(); // 錯誤換成預設值，但 4 還是沒有處理
	}

	// 要讓單筆錯誤不影響其他資料：在 flatMap 內部處理每一筆的錯誤
	@Test
	void onErrorResumeInsideFlatMap_shouldSkipFailedItemAndContinue() {
		Flux<Integer> flux = Flux.just(1, 2, 0, 4)
				.flatMap(i -> Mono.fromCallable(() -> divide(10, i))
						.onErrorResume(ArithmeticException.class, e -> Mono.empty()));

		StepVerifier.create(flux).expectNext(10, 5, 2).verifyComplete();
	}

	// onErrorMap：把技術性的例外轉成業務例外（之後由 @RestControllerAdvice 對應成 HTTP 狀態碼）
	@Test
	void onErrorMap_shouldTranslateException() {
		Mono<String> mono = Mono.<String>error(new IllegalArgumentException("db error"))
				.onErrorMap(IllegalArgumentException.class, e -> new UserNotFoundException("42"));

		StepVerifier.create(mono).expectErrorMessage("找不到使用者：42").verify();
	}

	// retry 重新訂閱：因為 Mono 是 Cold（第 1 課），重新訂閱就是從頭再執行一次
	@Test
	void retry_shouldResubscribe_whenErrorOccurs() {
		var attempts = new AtomicInteger();
		Mono<String> flaky = Mono.fromCallable(() -> {
			if (attempts.incrementAndGet() < 3) {
				throw new IllegalStateException("暫時失敗");
			}
			return "成功";
		});

		StepVerifier.create(flaky.retry(2)).expectNext("成功").verifyComplete();
		assertThat(attempts).hasValue(3);
	}

	// ---- 空值 ----

	@Test
	void just_shouldThrowImmediately_whenValueIsNull() {
		assertThatThrownBy(() -> Mono.just(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void justOrEmpty_shouldCompleteWithoutValue_whenValueIsNull() {
		StepVerifier.create(Mono.justOrEmpty(null)).verifyComplete();
	}

	// 空的 Mono 不會呼叫 map，直接完成。Controller 回傳空的 Mono 時，HTTP 回應是 200 且沒有內容，而不是 404
	@Test
	void map_shouldNotBeCalled_whenMonoIsEmpty() {
		var calls = new AtomicInteger();

		Mono<String> mono = Mono.<String>empty().map(s -> {
			calls.incrementAndGet();
			return s.toUpperCase();
		});

		StepVerifier.create(mono).verifyComplete();
		assertThat(calls).hasValue(0);
	}

	@Test
	void switchIfEmpty_shouldTurnEmptyIntoError() {
		Mono<String> mono = Mono.<String>empty().switchIfEmpty(Mono.error(new UserNotFoundException("42")));

		StepVerifier.create(mono).expectError(UserNotFoundException.class).verify();
	}

	@Test
	void defaultIfEmpty_shouldProvideDefaultValue() {
		StepVerifier.create(Mono.<String>empty().defaultIfEmpty("訪客")).expectNext("訪客").verifyComplete();
	}

	// switchIfEmpty 的參數是一般的 Java 運算式：不論是不是空的，fallback() 都會先被呼叫（第 1 課的 Mono.just 陷阱）
	@Test
	void switchIfEmpty_shouldCallFallbackEagerly_evenWhenNotEmpty() {
		var fallbackCalls = new AtomicInteger();

		Mono<String> mono = Mono.just("有資料").switchIfEmpty(fallback(fallbackCalls));

		StepVerifier.create(mono).expectNext("有資料").verifyComplete();
		assertThat(fallbackCalls).hasValue(1); // 沒用到，卻已經執行了（若 fallback 會查資料庫，就是多一次查詢）
	}

	@Test
	void switchIfEmptyWithDefer_shouldCallFallbackOnlyWhenEmpty() {
		var fallbackCalls = new AtomicInteger();

		Mono<String> mono = Mono.just("有資料").switchIfEmpty(Mono.defer(() -> fallback(fallbackCalls)));

		StepVerifier.create(mono).expectNext("有資料").verifyComplete();
		assertThat(fallbackCalls).hasValue(0);
	}

	private static int divide(int a, int b) {
		return a / b;
	}

	private static Mono<String> fallback(AtomicInteger calls) {
		calls.incrementAndGet();
		return Mono.just("預設值");
	}
}
