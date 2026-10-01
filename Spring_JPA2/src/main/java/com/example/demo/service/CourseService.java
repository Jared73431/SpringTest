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

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public CourseService(CourseRepository courseRepository, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseDTO getCourseById(Long id) {
        return courseRepository.findWithStudentsById(id)
                .map(CourseDTO::fromEntityWithStudents)
                .orElseThrow(() -> new ResourceNotFoundException("課程", id));
    }

    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO) {
        CoursePO course = courseDTO.toEntity();
        return CourseDTO.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        CoursePO course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("課程", id));
        courseDTO.updateEntity(course);
        return CourseDTO.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        CoursePO course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("課程", id));

        // Remove the course from all associated students first
        course.clearStudents();
        courseRepository.save(course);

        // Now delete the course
        courseRepository.delete(course);
    }

    @Transactional
    public CourseDTO addStudentToCourse(Long courseId, Long studentId) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        course.addStudent(student);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO removeStudentFromCourse(Long courseId, Long studentId) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("課程", courseId));

        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("學生", studentId));

        course.removeStudent(student);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    //批次新增學生到課程
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

    // 批次從課程中移除學生
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

    @Transactional(readOnly = true)
    public List<CourseDTO> getCoursesByStudentId(Long studentId) {
        // Check if student exists
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("學生", studentId);
        }

        return courseRepository.findByStudentId(studentId).stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
    }

}
