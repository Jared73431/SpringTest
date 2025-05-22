package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.StudentDTO;
import com.example.demo.service.StudentService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    StudentService studentService;

    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(studentService.getStudentById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@RequestBody StudentDTO studentDTO) {
        return new ResponseEntity<>(studentService.createStudent(studentDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @RequestBody StudentDTO studentDTO) {
        try {
            return ResponseEntity.ok(studentService.updateStudent(id, studentDTO));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量為學生添加課程
     * POST /api/students/{studentId}/courses/batch
     */
    @PostMapping("/{studentId}/courses/batch")
    public ResponseEntity<StudentDTO> addCoursesToStudent(
            @PathVariable Long studentId,
            @RequestBody List<Long> courseIds) {
        try {
            if (courseIds == null || courseIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            StudentDTO updatedStudent = studentService.addCoursesToStudent(studentId, courseIds);
            return ResponseEntity.ok(updatedStudent);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量從學生中移除課程
     * DELETE /api/students/{studentId}/courses/batch
     */
    @DeleteMapping("/{studentId}/courses/batch")
    public ResponseEntity<StudentDTO> removeCoursesFromStudent(
            @PathVariable Long studentId,
            @RequestBody List<Long> courseIds) {
        try {
            if (courseIds == null || courseIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            StudentDTO updatedStudent = studentService.removeCoursesFromStudent(studentId, courseIds);
            return ResponseEntity.ok(updatedStudent);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 清空學生的所有課程
     * DELETE /api/students/{studentId}/courses/all
     */
    @DeleteMapping("/{studentId}/courses/all")
    public ResponseEntity<StudentDTO> clearAllCoursesFromStudent(@PathVariable Long studentId) {
        try {
            StudentDTO updatedStudent = studentService.clearAllCoursesFromStudent(studentId);
            return ResponseEntity.ok(updatedStudent);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 為學生添加單一課程
     * POST /api/students/{studentId}/courses/{courseId}
     */
    @PostMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentDTO> addCourseToStudent(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(studentService.addCourseToStudent(studentId, courseId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 從學生中移除單一課程
     * DELETE /api/students/{studentId}/courses/{courseId}
     */
    @DeleteMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentDTO> removeCourseFromStudent(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(studentService.removeCourseFromStudent(studentId, courseId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根據課程ID查詢所有學生
     * GET /api/students/by-course/{courseId}
     */
    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<List<StudentDTO>> getStudentsByCourseId(@PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(studentService.getStudentsByCourseId(courseId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
