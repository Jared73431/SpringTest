package com.example.demo.controller;

import java.util.List;

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
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.service.StudentService;

import jakarta.validation.Valid;

/**
 * 學生 REST API，包含學生 CRUD 與學生 ↔ 課程關聯（單筆 / 批次 / 全部清除）的操作。
 * 不在這裡 try/catch，Service 拋出的例外由 GlobalExceptionHandler 統一轉成 ProblemDetail。
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** GET /api/students：取得所有學生（含課程 ID）。200。 */
    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    /** GET /api/students/{id}：取得單一學生與課程 ID。200；學生不存在 → 404。 */
    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    /** POST /api/students：建立學生。201（目前未附 Location header）；驗證失敗 → 400。 */
    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        return new ResponseEntity<>(studentService.createStudent(studentDTO), HttpStatus.CREATED);
    }

    /** PUT /api/students/{id}：更新學生名稱（不影響課程關聯）。200；驗證失敗 → 400；學生不存在 → 404。 */
    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDTO) {
        return ResponseEntity.ok(studentService.updateStudent(id, studentDTO));
    }

    /** DELETE /api/students/{id}：刪除學生（課程保留，只解除關聯）。204；學生不存在 → 404。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/students/{studentId}/courses/batch：批次為學生新增課程，Body 為課程 ID 陣列。
     * 200；空陣列或任一課程 ID 不存在 → 400（整批不新增）；學生不存在 → 404。
     */
    @PostMapping("/{studentId}/courses/batch")
    public ResponseEntity<StudentDTO> addCoursesToStudent(
            @PathVariable Long studentId,
            @RequestBody List<Long> courseIds) {
        // 空清單視為請求錯誤（CourseController 的批次 API 則允許空集合，兩者行為不一致）
        if (courseIds == null || courseIds.isEmpty()) {
            throw new InvalidRequestException("課程 ID 清單不可為空");
        }
        StudentDTO updatedStudent = studentService.addCoursesToStudent(studentId, courseIds);
        return ResponseEntity.ok(updatedStudent);
    }

    /**
     * DELETE /api/students/{studentId}/courses/batch：批次從學生中移除課程，Body 為課程 ID 陣列。
     * 200；空陣列或任一課程 ID 不存在 → 400；學生不存在 → 404。
     */
    @DeleteMapping("/{studentId}/courses/batch")
    public ResponseEntity<StudentDTO> removeCoursesFromStudent(
            @PathVariable Long studentId,
            @RequestBody List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            throw new InvalidRequestException("課程 ID 清單不可為空");
        }
        StudentDTO updatedStudent = studentService.removeCoursesFromStudent(studentId, courseIds);
        return ResponseEntity.ok(updatedStudent);
    }

    /**
     * DELETE /api/students/{studentId}/courses/all：清空學生的所有課程。
     * 200（回傳更新後的學生）；學生不存在 → 404。
     */
    @DeleteMapping("/{studentId}/courses/all")
    public ResponseEntity<StudentDTO> clearAllCoursesFromStudent(@PathVariable Long studentId) {
        StudentDTO updatedStudent = studentService.clearAllCoursesFromStudent(studentId);
        return ResponseEntity.ok(updatedStudent);
    }

    /**
     * POST /api/students/{studentId}/courses/{courseId}：為學生新增單一課程。
     * 200；學生或課程不存在 → 404。
     */
    @PostMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentDTO> addCourseToStudent(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(studentService.addCourseToStudent(studentId, courseId));
    }

    /**
     * DELETE /api/students/{studentId}/courses/{courseId}：從學生中移除單一課程。
     * 200；學生或課程不存在 → 404。
     */
    @DeleteMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentDTO> removeCourseFromStudent(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(studentService.removeCourseFromStudent(studentId, courseId));
    }

    /**
     * GET /api/students/by-course/{courseId}：根據課程ID查詢所有學生。
     * 200（可能為空陣列）；課程不存在 → 404。
     */
    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<List<StudentDTO>> getStudentsByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(studentService.getStudentsByCourseId(courseId));
    }
}
