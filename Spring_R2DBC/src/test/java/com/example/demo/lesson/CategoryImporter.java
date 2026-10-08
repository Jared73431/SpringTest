package com.example.demo.lesson;

import java.util.List;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.LessonCategories.Category;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 第 6 課：依序新增多個分類。importAll 沒有交易，importAllTransactional 有 @Transactional。
 *
 * <p>
 * Reactive 的 @Transactional 和 Spring MVC 一樣寫在方法上，但它包住的是「回傳的 Mono / Flux」：
 * 交易從訂閱時開始、在完成或錯誤時 commit / rollback，交易資訊放在 Reactor Context（第 9 課，Spring_Webflux），不是 ThreadLocal。
 */
@Component
public class CategoryImporter {

	private final R2dbcEntityTemplate template;

	public CategoryImporter(R2dbcEntityTemplate template) {
		this.template = template;
	}

	public Mono<Void> importAll(List<Category> categories) {
		return Flux.fromIterable(categories).concatMap(template::insert).then();
	}

	@Transactional
	public Mono<Void> importAllTransactional(List<Category> categories) {
		return importAll(categories);
	}
}
