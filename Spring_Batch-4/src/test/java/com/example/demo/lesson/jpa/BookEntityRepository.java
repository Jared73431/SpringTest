package com.example.demo.lesson.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** RepositoryItemReader 呼叫的 Repository */
public interface BookEntityRepository extends JpaRepository<BookEntity, Long> {

	/** 和修正前的 findBooksAfterYear 一樣：沒有 Pageable 參數、回傳 List（RepositoryItemReader 不能使用） */
	List<BookEntity> findByPublishedYearGreaterThanEqual(int year);
}
