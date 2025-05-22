package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.StudentDTO;
import com.example.demo.entity.CoursePO;
import com.example.demo.entity.StudentPO;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.StudentRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class StudentService {

    @Autowired
    CourseRepository courseRepository;
    @Autowired
    StudentRepository studentRepository;

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
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + id));
    }

    @Transactional
    public StudentDTO createStudent(StudentDTO studentDTO) {
        StudentPO student = studentDTO.toEntity();
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        StudentPO student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + id));
        studentDTO.updateEntity(student);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long id) {
        StudentPO student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + id));

        // Remove the student from all associated courses first
        student.clearCourses();
        studentRepository.save(student);

        // Now delete the student
        studentRepository.delete(student);
    }

    @Transactional
    public StudentDTO addCourseToStudent(Long studentId, Long courseId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        student.addCourse(course);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO removeCourseFromStudent(Long studentId, Long courseId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

        CoursePO course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        student.removeCourse(course);
        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getStudentsByCourseId(Long courseId) {
        // Check if course exists
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course not found with id: " + courseId);
        }

        return studentRepository.findByCourseId(courseId).stream()
                .map(StudentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentDTO addCoursesToStudent(Long studentId, List<Long> courseIds) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

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
            throw new EntityNotFoundException("Courses not found with ids: " + notFoundCourseIds);
        }

        // 使用 StudentPO 的 addCourses 方法批量添加課程
        student.addCourses(courses);

        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO removeCoursesFromStudent(Long studentId, List<Long> courseIds) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

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
            throw new EntityNotFoundException("Courses not found with ids: " + notFoundCourseIds);
        }

        // 使用 StudentPO 的 removeCourses 方法批量移除課程
        student.removeCourses(courses);

        return StudentDTO.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public StudentDTO clearAllCoursesFromStudent(Long studentId) {
        StudentPO student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

        // 使用 StudentPO 的 clearCourses 方法清空所有課程
        student.clearCourses();

        return StudentDTO.fromEntity(studentRepository.save(student));
    }
}
