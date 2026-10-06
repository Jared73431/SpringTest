package com.example.demo.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 第 3 課：忽略回傳值的 Bug（第 1 課的直接後果，也是 WebFlux 最常見的 Bug）。
 *
 * <p>
 * 直覺以為：{@code repository.save(user);} 這一行會存檔。<br>
 * 實際上：save 回傳的 Mono 沒有接到回傳的 chain 上，沒有人訂閱它，所以什麼都沒發生；而且<b>不會有任何錯誤或警告</b>。
 *
 * <p>
 * 原則：每一個 Mono / Flux 都必須接到最後回傳的那條 chain 上（flatMap、then、thenReturn、Mono.when…），
 * 讓框架的訂閱一路傳到它。
 */
class L03_IgnoredReturnValueTest {

	record User(String name) {
	}

	/** 模擬 Spring Data R2DBC 的 ReactiveCrudRepository：save 回傳 Mono，訂閱時才真的寫入 */
	static class InMemoryRepository<T> {

		private final List<T> saved = new CopyOnWriteArrayList<>();

		Mono<T> save(T entity) {
			return Mono.fromCallable(() -> {
				saved.add(entity);
				return entity;
			});
		}

		List<T> saved() {
			return saved;
		}
	}

	private final InMemoryRepository<User> users = new InMemoryRepository<>();
	private final InMemoryRepository<String> auditLogs = new InMemoryRepository<>();

	// ---- 錯誤寫法 ----

	private Mono<User> registerIgnoringSave(String name) {
		var user = new User(name);
		users.save(user); // 回傳的 Mono 被丟掉，永遠不會被訂閱
		return Mono.just(user);
	}

	private Mono<User> registerWithAuditInDoOnNext(String name) {
		return users.save(new User(name))
				// doOnNext 是「順便做點事」（例如 log），它不會訂閱 lambda 回傳的 Mono
				.doOnNext(user -> auditLogs.save("註冊：" + user.name()));
	}

	// ---- 正確寫法 ----

	private Mono<User> register(String name) {
		return users.save(new User(name));
	}

	private Mono<User> registerWithAudit(String name) {
		return users.save(new User(name))
				// flatMap 會訂閱 lambda 回傳的 Mono；thenReturn 等它完成後再把 user 傳下去
				.flatMap(user -> auditLogs.save("註冊：" + user.name()).thenReturn(user));
	}

	@Test
	void registerIgnoringSave_shouldReturnUserButSaveNothing() {
		StepVerifier.create(registerIgnoringSave("Amy")).expectNext(new User("Amy")).verifyComplete();

		assertThat(users.saved()).isEmpty(); // 回應看起來成功，資料卻沒有寫入
	}

	@Test
	void register_shouldSaveUser_whenSaveIsPartOfTheChain() {
		StepVerifier.create(register("Amy")).expectNext(new User("Amy")).verifyComplete();

		assertThat(users.saved()).containsExactly(new User("Amy"));
	}

	@Test
	void registerWithAuditInDoOnNext_shouldSaveUserButNotAuditLog() {
		StepVerifier.create(registerWithAuditInDoOnNext("Amy")).expectNextCount(1).verifyComplete();

		assertThat(users.saved()).hasSize(1);
		assertThat(auditLogs.saved()).isEmpty();
	}

	@Test
	void registerWithAudit_shouldSaveBoth_whenUsingFlatMap() {
		StepVerifier.create(registerWithAudit("Amy")).expectNext(new User("Amy")).verifyComplete();

		assertThat(users.saved()).containsExactly(new User("Amy"));
		assertThat(auditLogs.saved()).containsExactly("註冊：Amy");
	}
}
