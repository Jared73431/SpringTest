package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.CoursePO;

/**
 * 課程的 Spring Data JPA Repository：繼承 JpaRepository 即取得 CRUD、分頁、排序，
 * Spring 在啟動時自動產生實作類別，不需要自己寫 DAO。
 * 示範三種查詢方式：方法名稱推導（derived query）、@Query JPQL、@EntityGraph 預先載入關聯。
 */
@Repository
public interface CourseRepository extends JpaRepository<CoursePO, Long> {

    // Find course by ID and eagerly fetch students
    // @EntityGraph 讓這次查詢以 JOIN 一併載入 LAZY 的 students，
    // 避免交易外存取時的 LazyInitializationException，也避免逐筆載入的 N+1 Query。
    // 方法名稱中 find 與 By 之間的 "WithStudents" 只是描述用文字，Spring Data 解析時會忽略，實際條件只有 ById
    @EntityGraph(attributePaths = {"students"})
    Optional<CoursePO> findWithStudentsById(Long id);

    // Find courses by student ID
    // JPQL 查詢的是 Entity 與屬性（CoursePO、c.students），不是資料表名稱；
    // 透過關聯 JOIN 時不需要寫出中間表 selected_course
    @Query("SELECT c FROM CoursePO c JOIN c.students s WHERE s.id = :studentId")
    List<CoursePO> findByStudentId(@Param("studentId") Long studentId);

    // Find courses with specific point value
    // Derived query：Spring Data 依方法名稱（findBy + 屬性名 + 條件關鍵字）自動產生 JPQL
    List<CoursePO> findByPoint(int point);

    // Find courses with point value greater than or equal to given value
    List<CoursePO> findByPointGreaterThanEqual(int minPoint);

    // Find courses by name containing (case insensitive)
    // 以具名參數 :keyword 綁定，值不會被拼接進 SQL 字串，可避免 SQL Injection。
    // 註：此方法名稱本身也符合 derived query 規則，但有 @Query 時以 @Query 為準
    @Query("SELECT c FROM CoursePO c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<CoursePO> findByNameContainingIgnoreCase(@Param("keyword") String keyword);

    // Count courses by student ID
    @Query("SELECT COUNT(c) FROM CoursePO c JOIN c.students s WHERE s.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
}
