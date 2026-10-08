package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

import com.example.demo.lesson.LessonCategories.Category;
import com.example.demo.lesson.LessonCategories.PersistableCategory;
import com.example.demo.lesson.LessonCategories.VersionedCategory;

import reactor.test.StepVerifier;

/**
 * 第 4 課：主鍵由程式指定時，save() 會變成 UPDATE。
 *
 * <p>
 * 直覺以為：save() 新的物件就會 INSERT。<br>
 * 實際上：save() 用 isNew() 判斷，預設是「id 為 null 才是新的」。id 一開始就有值，save() 會執行 UPDATE，
 * 更新 0 筆，資料沒有寫入卻不會出錯。Product 的 id 由資料庫產生（SERIAL），所以沒有這個問題。
 *
 * <pre>
 * 解法                       說明
 * template.insert()          明確指定 INSERT，最直接
 * 實作 Persistable            自己決定 isNew()（修正前的 Product 就是這樣寫，但它的 id 是資料庫產生的，根本不需要）
 * 加上 @Version               version 為 null 時視為新的，順便得到樂觀鎖
 * </pre>
 */
class L04_PersistableTest extends LessonTestBase {

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private PersistableCategoryRepository persistableRepository;

	@Autowired
	private VersionedCategoryRepository versionedRepository;

	@Autowired
	private R2dbcEntityTemplate template;

	// 最危險的地方：save() 回傳了物件，看起來成功了，資料卻沒有寫入，而且沒有任何錯誤。
	// （較舊的 Spring Data R2DBC 會丟出「Row with Id [...] does not exist」，目前的版本則是默默更新 0 筆）
	@Test
	void save_shouldSilentlyWriteNothing_whenIdIsAssignedByApplication() {
		var category = new Category("PERIPHERAL", "周邊設備", CategoryStatus.ACTIVE);

		StepVerifier.create(categoryRepository.save(category)).expectNext(category).verifyComplete();

		StepVerifier.create(categoryRepository.count()).expectNext(0L).verifyComplete();
	}

	@Test
	void templateInsert_shouldAlwaysInsert() {
		var category = new Category("PERIPHERAL", "周邊設備", CategoryStatus.ACTIVE);

		StepVerifier.create(template.insert(category).then(categoryRepository.findById("PERIPHERAL")))
				.expectNext(category)
				.verifyComplete();
	}

	@Test
	void persistable_shouldInsertNewAndUpdateLoaded() {
		StepVerifier.create(persistableRepository.save(PersistableCategory.newCategory("PERIPHERAL", "周邊設備")))
				.expectNextCount(1)
				.verifyComplete();

		// 從資料庫讀出來的 isNew() 是 false，再 save() 就是 UPDATE
		StepVerifier.create(persistableRepository.findById("PERIPHERAL")
				.map(loaded -> new PersistableCategory(loaded.getId(), "電腦周邊", CategoryStatus.ACTIVE))
				.flatMap(persistableRepository::save)
				.then(persistableRepository.findById("PERIPHERAL"))
				.map(PersistableCategory::name))
				.expectNext("電腦周邊")
				.verifyComplete();
	}

	@Test
	void version_shouldDecideNewByVersionAndDetectConcurrentUpdate() {
		StepVerifier.create(versionedRepository.save(new VersionedCategory("PERIPHERAL", "周邊設備", CategoryStatus.ACTIVE, null)))
				.assertNext(saved -> assertThat(saved.version()).isZero()) // INSERT，版本從 0 開始
				.verifyComplete();

		// 兩個請求讀到同一個版本，先存的成功，後存的因為版本不符而失敗（樂觀鎖）
		VersionedCategory first = versionedRepository.findById("PERIPHERAL").block();
		VersionedCategory second = versionedRepository.findById("PERIPHERAL").block();
		first.rename("電腦周邊");
		second.rename("其他");

		StepVerifier.create(versionedRepository.save(first))
				.assertNext(saved -> assertThat(saved.version()).isEqualTo(1))
				.verifyComplete();
		StepVerifier.create(versionedRepository.save(second))
				.expectError(OptimisticLockingFailureException.class)
				.verify();
	}
}
