package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.StudentPO;

/**
 * 學生的 Spring Data JPA Repository，查詢方式與 CourseRepository 對稱。
 */
@Repository
public interface StudentRepository extends JpaRepository<StudentPO, Long> {

    // 清空中間表 selected_course（測試用來清除資料）。
    // 中間表沒有對應的 Entity，無法用 JPQL 操作，因此使用 native SQL。
    // @Modifying：告訴 Spring Data 這是 INSERT/UPDATE/DELETE 而非 SELECT（改用 executeUpdate 執行）；
    // 修改資料需要交易，所以加上 @Transactional。
    // 注意：直接執行 SQL 會繞過 Persistence Context，記憶體中已載入的 Entity 不會同步更新
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM selected_course", nativeQuery = true)
    void deleteAllFromSelectedCourse();

    // Find student by ID and eagerly fetch courses
    // @EntityGraph 在這次查詢中一併載入 LAZY 的 courses，避免 LazyInitializationException 與 N+1 Query
    @EntityGraph(attributePaths = {"courses"})
    Optional<StudentPO> findWithCoursesById(Long id);

    // Find students by course ID
    @Query("SELECT s FROM StudentPO s JOIN s.courses c WHERE c.id = :courseId")
    List<StudentPO> findByCourseId(@Param("courseId") Long courseId);

    // Find students by name containing (case insensitive)
    @Query("SELECT s FROM StudentPO s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<StudentPO> findByNameContainingIgnoreCase(@Param("keyword") String keyword);

    // Count students by course ID
    @Query("SELECT COUNT(s) FROM StudentPO s JOIN s.courses c WHERE c.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
