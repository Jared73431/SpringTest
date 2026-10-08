package com.example.demo.lesson;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.demo.lesson.LessonCategories.Category;
import com.example.demo.lesson.LessonCategories.PersistableCategory;
import com.example.demo.lesson.LessonCategories.VersionedCategory;

/**
 * 第 4 課用的 Repository。Spring Data 預設不會掃描巢狀的 Repository 介面，所以各自宣告成頂層介面（package-private）。
 */
interface CategoryRepository extends ReactiveCrudRepository<Category, String> {
}

interface PersistableCategoryRepository extends ReactiveCrudRepository<PersistableCategory, String> {
}

interface VersionedCategoryRepository extends ReactiveCrudRepository<VersionedCategory, String> {
}
