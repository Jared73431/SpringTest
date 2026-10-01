package com.example.demo.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.StudentDTO;
import com.example.demo.entity.CoursePO;
import com.example.demo.entity.StudentPO;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.StudentRepository;

/**
 * 學生 CRUD 與「學生 ↔ 課程」多對多關聯的維護，對外回傳 StudentDTO。
 * StudentPO 是多對多的被擁有方（mappedBy），所以關聯變動必須透過 StudentPO 的便利方法同步更新 CoursePO.students，
 * 否則中間表 selected_course 不會被寫入。
 */
@Service
public class StudentService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public StudentService(CourseRepository courseRepository, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * 取得所有學生與各自的課程 ID。
     * [Learning] StudentDTO.fromEntity 會讀取 LAZY 的 courses，每位學生各多一次查詢（N+1），資料量大時可改用 fetch join。
     */
    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(StudentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /** 取得單一學生與其課程（一次載入 courses）。學生不存在 → ResourceNotFoundException（404）。 */
    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long id) {
        return studentRepository.findWithCoursesById(id)
                .map(StudentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("學生", id));
    }

    /** 建立學生（不處理課程關聯）。 */
    @Transactional
    public StudentDTO createStudent(StudentDTO studentDTO) {
        StudentPO student = studentDTO.toEntity();
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    /**
     * 更新學生基本資料（只更新 name，不改動課程關聯）；先讀出既有 Entity 再覆寫欄位。
     * 學生不存在 → ResourceNotFoundException（404）。
     */
    @Transactional
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        StudentPO student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("學生", id));
        studentDTO.updateEntity(student);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    /**
     * 刪除學生，課程本身不會被刪除。學生不存在 → ResourceNotFoundException（404）。
     */
    @Transactional
    public void deleteStudent(Long id) {
        StudentPO student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("學生", id));

        // 先從所有課程（擁有方）的 students 集合移除此學生，Hibernate 才會刪除中間表的關聯資料；
        // 學生是被擁有方，若直接刪除，中間表仍參照此學生，會違反外鍵限制
        student.clearCourses();
        studentRepository.save(student);

        // 關聯解除後再刪除學生本身
        studentRepository.delete(student);
    }

    /** 為學生加選單一課程。學生或課程不存在 → ResourceNotFoundException（404），兩者的 ID 都在 URL 中。 */
    @Transactional
    public StudentDTO addCourseToStudent(Long studentId, Long courseId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        student.addCourse(course);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    /** 為學生退選單一課程（只刪除中間表關聯）。學生或課程不存在 → ResourceNotFoundException（404）。 */
    @Transactional
    public StudentDTO removeCourseFromStudent(Long studentId, Long courseId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        student.removeCourse(course);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    /**
     * 查詢修習某課程的所有學生。
     * 課程不存在 → ResourceNotFoundException（404）；課程存在但沒有學生 → 回傳空 List。
     */
    @Transactional(readOnly = true)
    public List<StudentDTO> getStudentsByCourseId(Long courseId) {
        // 先確認課程存在：否則「課程不存在」與「課程沒有學生」都會回傳空 List，用戶端無法區分
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("課程", courseId);
        }

        return studentRepository.findByCourseId(courseId).stream()
                .map(StudentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批次為學生加選課程。
     * 學生不存在 → ResourceNotFoundException（404）；Body 中有任一課程 ID 不存在 → InvalidRequestException（400），整批都不加選。
     * 重複的課程 ID 視為同一門課。
     */
    @Transactional
    public StudentDTO addCoursesToStudent(Long studentId, List<Long> courseIds) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        // 重複的課程 ID 視為同一門課：先去除重複再查詢與比對數量
        // （修正前直接用 List 比對，重複 ID 會被誤判為「課程不存在: []」）
        Set<Long> uniqueCourseIds = new LinkedHashSet<>(courseIds);

        // 根據課程ID列表查找所有課程
        List<CoursePO> courses = courseRepository.findAllById(uniqueCourseIds);

        // 檢查是否所有課程都存在
        if (courses.size() != uniqueCourseIds.size()) {
            List<Long> foundCourseIds = courses.stream()
                    .map(CoursePO::getId)
                    .collect(Collectors.toList());
            List<Long> notFoundCourseIds = uniqueCourseIds.stream()
                    .filter(id -> !foundCourseIds.contains(id))
                    .collect(Collectors.toList());
            throw new InvalidRequestException("課程不存在: " + notFoundCourseIds);
        }

        // 使用 StudentPO 的 addCourses 方法批次新增課程
        student.addCourses(courses);

        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    /**
     * 批次為學生退選課程。
     * 學生不存在 → ResourceNotFoundException（404）；Body 中有任一課程 ID 不存在 → InvalidRequestException（400）。
     * 與 addCoursesToStudent 相同，courseIds 含重複 ID 時會被誤判為不存在。
     */
    @Transactional
    public StudentDTO removeCoursesFromStudent(Long studentId, List<Long> courseIds) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        // 重複的課程 ID 視為同一門課：先去除重複再查詢與比對數量
        // （修正前直接用 List 比對，重複 ID 會被誤判為「課程不存在: []」）
        Set<Long> uniqueCourseIds = new LinkedHashSet<>(courseIds);

        // 根據課程ID列表查找所有課程
        List<CoursePO> courses = courseRepository.findAllById(uniqueCourseIds);

        // 檢查是否所有課程都存在
        if (courses.size() != uniqueCourseIds.size()) {
            List<Long> foundCourseIds = courses.stream()
                    .map(CoursePO::getId)
                    .collect(Collectors.toList());
            List<Long> notFoundCourseIds = uniqueCourseIds.stream()
                    .filter(id -> !foundCourseIds.contains(id))
                    .collect(Collectors.toList());
            throw new InvalidRequestException("課程不存在: " + notFoundCourseIds);
        }

        // 使用 StudentPO 的 removeCourses 方法批次移除課程
        student.removeCourses(courses);

        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    /** 清空學生的所有選課（學生與課程本身都保留）。學生不存在 → ResourceNotFoundException（404）。 */
    @Transactional
    public StudentDTO clearAllCoursesFromStudent(Long studentId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        // 使用 StudentPO 的 clearCourses 方法清空所有課程
        student.clearCourses();

        return StudentDTO.fromEntity(studentRepository.save(student));
    }
}
