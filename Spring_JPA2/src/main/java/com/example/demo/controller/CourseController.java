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

import jakarta.validation.Valid;

/**
 * 課程 REST API，包含課程 CRUD 與課程 ↔ 學生關聯（單筆 / 批次）的操作。
 * 不在這裡 try/catch，Service 拋出的例外由 GlobalExceptionHandler 統一轉成 ProblemDetail。
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** GET /api/courses：取得所有課程（含學生 ID）。200。 */
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /** GET /api/courses/{id}：取得課程與學生詳細資料。200；課程不存在 → 404。 */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    /**
     * POST /api/courses：建立課程。201；驗證失敗 → 400。
     * 目前沒有附上 Location header（ProductController / OrderController 有），屬於 API 風格不一致之處。
     */
    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody CourseDTO courseDTO) {
        return new ResponseEntity<>(courseService.createCourse(courseDTO), HttpStatus.CREATED);
    }

    /** PUT /api/courses/{id}：更新課程名稱與學分（不影響學生關聯）。200；驗證失敗 → 400；課程不存在 → 404。 */
    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseDTO courseDTO) {
        return ResponseEntity.ok(courseService.updateCourse(id, courseDTO));
    }

    /** DELETE /api/courses/{id}：刪除課程（學生保留，只解除關聯）。204；課程不存在 → 404。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // 批次學生操作
    // 路徑 /students/batch 與 /students/{studentId} 不會衝突：Spring 會優先選擇較明確的字面路徑
    /**
     * POST /api/courses/{courseId}/students/batch：批次將學生加入課程，Body 為學生 ID 陣列。
     * 200；課程不存在 → 404；任一學生 ID 不存在 → 400（整批不新增）。空陣列不會報錯，等同不做任何事。
     */
    @PostMapping("/{courseId}/students/batch")
    public ResponseEntity<CourseDTO> addStudentsToCourse(
            @PathVariable Long courseId,
            @RequestBody Set<Long> studentIds) {
        return ResponseEntity.ok(courseService.addStudentsToCourse(courseId, studentIds));
    }

    /** POST /api/courses/{courseId}/students/{studentId}：將單一學生加入課程。200；課程或學生不存在 → 404。 */
    @PostMapping("/{courseId}/students/{studentId}")
    public ResponseEntity<CourseDTO> addStudentToCourse(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(courseService.addStudentToCourse(courseId, studentId));
    }

    /**
     * DELETE /api/courses/{courseId}/students/batch：批次將學生移出課程，Body 為學生 ID 陣列。
     * 200；課程不存在 → 404；任一學生 ID 不存在 → 400。
     * [Learning] DELETE 帶 Request Body 在 HTTP 規範中沒有明確語意，部分用戶端或 Proxy 會忽略它。
     */
    @DeleteMapping("/{courseId}/students/batch")
    public ResponseEntity<CourseDTO> removeStudentsFromCourse(
            @PathVariable Long courseId,
            @RequestBody Set<Long> studentIds) {
        return ResponseEntity.ok(courseService.removeStudentsFromCourse(courseId, studentIds));
    }

    /** DELETE /api/courses/{courseId}/students/{studentId}：將單一學生移出課程。200（回傳更新後的課程）；課程或學生不存在 → 404。 */
    @DeleteMapping("/{courseId}/students/{studentId}")
    public ResponseEntity<CourseDTO> removeStudentFromCourse(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(courseService.removeStudentFromCourse(courseId, studentId));
    }

    /** GET /api/courses/by-student/{studentId}：查詢學生修習的課程。200（可能為空陣列）；學生不存在 → 404。 */
    @GetMapping("/by-student/{studentId}")
    public ResponseEntity<List<CourseDTO>> getCoursesByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(courseService.getCoursesByStudentId(studentId));
    }
}
