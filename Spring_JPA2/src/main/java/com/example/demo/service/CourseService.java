package com.example.demo.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CourseDTO;
import com.example.demo.entity.CoursePO;
import com.example.demo.entity.StudentPO;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.StudentRepository;

/**
 * 課程 CRUD 與「課程 ↔ 學生」多對多關聯的維護，對外回傳 CourseDTO。
 * Entity 轉 DTO 在 @Transactional 方法內完成，讀取 LAZY 的 students 集合時 Session 仍然開著。
 * CoursePO 是多對多的擁有方，中間表 selected_course 的寫入以 course.students 為準。
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public CourseService(CourseRepository courseRepository, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * 取得所有課程（只含學生 ID，不含學生詳細資料）。readOnly 交易：Hibernate 可略過 dirty checking。
     * [Learning] fromEntity 會讀取 LAZY 的 students，每門課程各多一次查詢（N+1），資料量大時可改用 fetch join。
     */
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 取得單一課程與其學生清單；用 findWithStudentsById 一次載入學生，避免 LAZY 集合再多發 SQL。
     * 課程不存在 → ResourceNotFoundException（404）。
     */
    @Transactional(readOnly = true)
    public CourseDTO getCourseById(Long id) {
        return courseRepository.findWithStudentsById(id)
                .map(CourseDTO::fromEntityWithStudents)
                .orElseThrow(() -> new ResourceNotFoundException("課程", id));
    }

    /** 建立課程（不處理學生關聯）。 */
    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO) {
        CoursePO course = courseDTO.toEntity();
        return CourseDTO.fromEntity(courseRepository.save(course));
    }

    /**
     * 更新課程基本資料；先讀出既有 Entity 再覆寫欄位，才不會因為從 DTO 新建物件而遺失既有的學生關聯。
     * 課程不存在 → ResourceNotFoundException（404）。
     */
    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        CoursePO course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("課程", id));
        courseDTO.updateEntity(course);
        return CourseDTO.fromEntity(courseRepository.save(course));
    }

    /**
     * 刪除課程，學生本身不會被刪除（未設定 CascadeType.REMOVE）。
     * 課程不存在 → ResourceNotFoundException（404）。
     */
    @Transactional
    public void deleteCourse(Long id) {
        CoursePO course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("課程", id));

        // 先解除與所有學生的關聯：清掉中間表 selected_course 的資料，
        // 並同步移除學生端 courses 集合中的參照，讓雙向關聯在記憶體中也保持一致
        course.clearStudents();
        courseRepository.save(course);

        // 關聯解除後再刪除課程本身
        courseRepository.delete(course);
    }

    /**
     * 將單一學生加入課程。課程或學生不存在 → ResourceNotFoundException（404），兩者的 ID 都在 URL 中。
     */
    @Transactional
    public CourseDTO addStudentToCourse(Long courseId, Long studentId) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        course.addStudent(student);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    /**
     * 將單一學生移出課程（只刪除中間表關聯）。課程或學生不存在 → ResourceNotFoundException（404）。
     */
    @Transactional
    public CourseDTO removeStudentFromCourse(Long courseId, Long studentId) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        course.removeStudent(student);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    /**
     * 批次新增學生到課程。
     * 課程不存在 → ResourceNotFoundException（404）；Body 中有任一學生 ID 不存在 → InvalidRequestException（400），整批都不新增。
     */
    @Transactional
    public CourseDTO addStudentsToCourse(Long courseId, Set<Long> studentIds) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        // 批次查找學生
        List<StudentPO> students = studentRepository.findAllById(studentIds);

        // 檢查是否所有學生都存在
        if (students.size() != studentIds.size()) {
            Set<Long> foundIds = students.stream().map(StudentPO::getId).collect(Collectors.toSet());
            Set<Long> notFoundIds = studentIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new InvalidRequestException("學生不存在: " + notFoundIds);
        }

        // 使用 addStudents 方法批次新增
        course.addStudents(students);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    /**
     * 批次從課程中移除學生。
     * 課程不存在 → ResourceNotFoundException（404）；Body 中有任一學生 ID 不存在 → InvalidRequestException（400）。
     */
    @Transactional
    public CourseDTO removeStudentsFromCourse(Long courseId, Set<Long> studentIds) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        // 批次查找學生
        List<StudentPO> students = studentRepository.findAllById(studentIds);

        // 檢查是否所有學生都存在
        if (students.size() != studentIds.size()) {
            Set<Long> foundIds = students.stream().map(StudentPO::getId).collect(Collectors.toSet());
            Set<Long> notFoundIds = studentIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new InvalidRequestException("學生不存在: " + notFoundIds);
        }

        // 使用 removeStudents 方法批次移除
        course.removeStudents(students);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    /**
     * 查詢某位學生修習的所有課程。
     * 學生不存在 → ResourceNotFoundException（404）；學生存在但沒有課程 → 回傳空 List。
     */
    @Transactional(readOnly = true)
    public List<CourseDTO> getCoursesByStudentId(Long studentId) {
        // 先確認學生存在：否則「學生不存在」與「學生沒有修課」都會回傳空 List，用戶端無法區分
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("學生", studentId);
        }

        return courseRepository.findByStudentId(studentId).stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
    }

}
