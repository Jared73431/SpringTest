package com.example.demo.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CourseDTO;
import com.example.demo.entity.CoursePO;
import com.example.demo.entity.StudentPO;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.StudentRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CurseService {

    @Autowired
    CourseRepository courseRepository;
    @Autowired
    StudentRepository studentRepository;

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
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
    }

    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO) {
        CoursePO course = courseDTO.toEntity();
        return CourseDTO.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        CoursePO course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
        courseDTO.updateEntity(course);
        return CourseDTO.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        CoursePO course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));

        // Remove the course from all associated students first
        course.clearStudents();
        courseRepository.save(course);

        // Now delete the course
        courseRepository.delete(course);
    }

    @Transactional
    public CourseDTO addStudentToCourse(Long courseId, Long studentId) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

        course.addStudent(student);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO removeStudentFromCourse(Long courseId, Long studentId) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

        course.removeStudent(student);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    //批量添加学生到课程
    @Transactional
    public CourseDTO addStudentsToCourse(Long courseId, Set<Long> studentIds) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // 批量查找学生
        List<StudentPO> students = studentRepository.findAllById(studentIds);

        // 检查是否所有学生都存在
        if (students.size() != studentIds.size()) {
            Set<Long> foundIds = students.stream().map(StudentPO::getId).collect(Collectors.toSet());
            Set<Long> notFoundIds = studentIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new EntityNotFoundException("Students not found with ids: " + notFoundIds);
        }

        // 使用 addStudents 方法批量添加
        course.addStudents(students);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    // 批量从课程中移除学生
    @Transactional
    public CourseDTO removeStudentsFromCourse(Long courseId, Set<Long> studentIds) {
        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // 批量查找学生
        List<StudentPO> students = studentRepository.findAllById(studentIds);

        // 检查是否所有学生都存在
        if (students.size() != studentIds.size()) {
            Set<Long> foundIds = students.stream().map(StudentPO::getId).collect(Collectors.toSet());
            Set<Long> notFoundIds = studentIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new EntityNotFoundException("Students not found with ids: " + notFoundIds);
        }

        // 使用 removeStudents 方法批量移除
        course.removeStudents(students);
        return CourseDTO.fromEntityWithStudents(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getCoursesByStudentId(Long studentId) {
        // Check if student exists
        if (!studentRepository.existsById(studentId)) {
            throw new EntityNotFoundException("Student not found with id: " + studentId);
        }

        return courseRepository.findByStudentId(studentId).stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
    }


}
