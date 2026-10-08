package com.example.demo.lesson;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.example.demo.lesson.LessonCategories.Category;

import reactor.test.StepVerifier;

/**
 * 第 6 課：Reactive 交易。
 *
 * <p>
 * 匯入 3 個分類，第 3 個的代碼和第 1 個重複（主鍵衝突）。沒有交易時，前 2 筆已經寫入；有交易時全部 rollback。
 *
 * <pre>
 * 寫法                      適用
 * @Transactional            Service 的方法，和 Spring MVC 的寫法相同（方法必須回傳 Mono / Flux）
 * TransactionalOperator     用程式指定交易範圍：operator.transactional(mono)，不需要 Spring Bean 與代理
 * </pre>
 */
class L06_TransactionTest extends LessonTestBase {

	private static final List<Category> CATEGORIES_WITH_DUPLICATE = List.of(
			new Category("PERIPHERAL", "周邊設備", CategoryStatus.ACTIVE),
			new Category("DISPLAY", "顯示器", CategoryStatus.ACTIVE),
			new Category("PERIPHERAL", "重複的代碼", CategoryStatus.ACTIVE));

	@Autowired
	private CategoryImporter importer;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TransactionalOperator transactionalOperator;

	@Test
	void withoutTransaction_shouldKeepEarlierInserts_whenLaterInsertFails() {
		StepVerifier.create(importer.importAll(CATEGORIES_WITH_DUPLICATE))
				.expectError(DataIntegrityViolationException.class)
				.verify();

		StepVerifier.create(categoryRepository.count()).expectNext(2L).verifyComplete(); // 匯入一半
	}

	@Test
	void transactionalAnnotation_shouldRollbackAll_whenAnyInsertFails() {
		StepVerifier.create(importer.importAllTransactional(CATEGORIES_WITH_DUPLICATE))
				.expectError(DataIntegrityViolationException.class)
				.verify();

		StepVerifier.create(categoryRepository.count()).expectNext(0L).verifyComplete();
	}

	@Test
	void transactionalOperator_shouldRollbackAll_whenAnyInsertFails() {
		StepVerifier.create(transactionalOperator.transactional(importer.importAll(CATEGORIES_WITH_DUPLICATE)))
				.expectError(DataIntegrityViolationException.class)
				.verify();

		StepVerifier.create(categoryRepository.count()).expectNext(0L).verifyComplete();
	}

	@Test
	void transaction_shouldCommit_whenAllInsertsSucceed() {
		StepVerifier.create(importer.importAllTransactional(CATEGORIES_WITH_DUPLICATE.subList(0, 2))).verifyComplete();

		StepVerifier.create(categoryRepository.count()).expectNext(2L).verifyComplete();
	}
}
