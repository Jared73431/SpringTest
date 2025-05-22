package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.CoursePO;

@Repository
public interface CourseRepository extends JpaRepository<CoursePO, Long> {

    // Find course by ID and eagerly fetch students
    @EntityGraph(attributePaths = {"students"})
    Optional<CoursePO> findWithStudentsById(Long id);

    // Find courses by student ID
    @Query("SELECT c FROM CoursePO c JOIN c.students s WHERE s.id = :studentId")
    List<CoursePO> findByStudentId(@Param("studentId") Long studentId);

    // Find courses with specific point value
    List<CoursePO> findByPoint(int point);

    // Find courses with point value greater than or equal to given value
    List<CoursePO> findByPointGreaterThanEqual(int minPoint);

    // Find courses by name containing (case insensitive)
    @Query("SELECT c FROM CoursePO c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<CoursePO> findByNameContainingIgnoreCase(@Param("keyword") String keyword);

    // Count courses by student ID
    @Query("SELECT COUNT(c) FROM CoursePO c JOIN c.students s WHERE s.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
}
