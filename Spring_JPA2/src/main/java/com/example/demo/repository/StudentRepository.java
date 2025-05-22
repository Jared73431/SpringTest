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

@Repository
public interface StudentRepository extends JpaRepository<StudentPO, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM selected_course", nativeQuery = true)
    void deleteAllFromSelectedCourse();

    // Find student by ID and eagerly fetch courses
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
