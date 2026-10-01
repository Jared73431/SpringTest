package com.example.demo.service;

import java.util.List;
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

@Service
public class StudentService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public StudentService(CourseRepository courseRepository, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(StudentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long id) {
        return studentRepository.findWithCoursesById(id)
                .map(StudentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("學生", id));
    }

    @Transactional
    public StudentDTO createStudent(StudentDTO studentDTO) {
        StudentPO student = studentDTO.toEntity();
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        StudentPO student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("學生", id));
        studentDTO.updateEntity(student);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long id) {
        StudentPO student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("學生", id));

        // Remove the student from all associated courses first
        student.clearCourses();
        studentRepository.save(student);

        // Now delete the student
        studentRepository.delete(student);
    }

    @Transactional
    public StudentDTO addCourseToStudent(Long studentId, Long courseId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        student.addCourse(course);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO removeCourseFromStudent(Long studentId, Long courseId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        student.removeCourse(course);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getStudentsByCourseId(Long courseId) {
        // Check if course exists
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("課程", courseId);
        }

        return studentRepository.findByCourseId(courseId).stream()
                .map(StudentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentDTO addCoursesToStudent(Long studentId, List<Long> courseIds) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        // 根據課程ID列表查找所有課程
        List<CoursePO> courses = courseRepository.findAllById(courseIds);

        // 檢查是否所有課程都存在
        if (courses.size() != courseIds.size()) {
            List<Long> foundCourseIds = courses.stream()
                    .map(CoursePO::getId)
                    .collect(Collectors.toList());
            List<Long> notFoundCourseIds = courseIds.stream()
                    .filter(id -> !foundCourseIds.contains(id))
                    .collect(Collectors.toList());
            throw new InvalidRequestException("課程不存在: " + notFoundCourseIds);
        }

        // 使用 StudentPO 的 addCourses 方法批次新增課程
        student.addCourses(courses);

        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO removeCoursesFromStudent(Long studentId, List<Long> courseIds) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        // 根據課程ID列表查找所有課程
        List<CoursePO> courses = courseRepository.findAllById(courseIds);

        // 檢查是否所有課程都存在
        if (courses.size() != courseIds.size()) {
            List<Long> foundCourseIds = courses.stream()
                    .map(CoursePO::getId)
                    .collect(Collectors.toList());
            List<Long> notFoundCourseIds = courseIds.stream()
                    .filter(id -> !foundCourseIds.contains(id))
                    .collect(Collectors.toList());
            throw new InvalidRequestException("課程不存在: " + notFoundCourseIds);
        }

        // 使用 StudentPO 的 removeCourses 方法批次移除課程
        student.removeCourses(courses);

        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO clearAllCoursesFromStudent(Long studentId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        // 使用 StudentPO 的 clearCourses 方法清空所有課程
        student.clearCourses();

        return StudentDTO.fromEntity(studentRepository.save(student));
    }
}
