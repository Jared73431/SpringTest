package com.example.demo.controller;

import java.util.List;
import java.util.Set;

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

import com.example.demo.dto.CourseDTO;
import com.example.demo.service.CourseService;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(@RequestBody CourseDTO courseDTO) {
        return new ResponseEntity<>(courseService.createCourse(courseDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @RequestBody CourseDTO courseDTO) {
        return ResponseEntity.ok(courseService.updateCourse(id, courseDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // 批次學生操作
    @PostMapping("/{courseId}/students/batch")
    public ResponseEntity<CourseDTO> addStudentsToCourse(
            @PathVariable Long courseId,
            @RequestBody Set<Long> studentIds) {
        return ResponseEntity.ok(courseService.addStudentsToCourse(courseId, studentIds));
    }

    @PostMapping("/{courseId}/students/{studentId}")
    public ResponseEntity<CourseDTO> addStudentToCourse(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(courseService.addStudentToCourse(courseId, studentId));
    }

    @DeleteMapping("/{courseId}/students/batch")
    public ResponseEntity<CourseDTO> removeStudentsFromCourse(
            @PathVariable Long courseId,
            @RequestBody Set<Long> studentIds) {
        return ResponseEntity.ok(courseService.removeStudentsFromCourse(courseId, studentIds));
    }

    @DeleteMapping("/{courseId}/students/{studentId}")
    public ResponseEntity<CourseDTO> removeStudentFromCourse(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(courseService.removeStudentFromCourse(courseId, studentId));
    }

    @GetMapping("/by-student/{studentId}")
    public ResponseEntity<List<CourseDTO>> getCoursesByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(courseService.getCoursesByStudentId(studentId));
    }
}
