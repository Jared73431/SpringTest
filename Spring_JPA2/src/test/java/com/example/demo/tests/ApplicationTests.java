package com.example.demo.tests;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.CoursePO;
import com.example.demo.entity.StudentPO;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.StudentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ApplicationTests {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    @Transactional
    public void clearDB() {
        studentRepository.deleteAllFromSelectedCourse();
        studentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)// 添加事务注解，确保在测试期间会话保持打开
    @Commit // 或使用舊版的 @Rollback(false)
    public void testManyToManyRelation() {
        // create courses
        CoursePO course1 = CoursePO.of("英文", 3);
        CoursePO course2 = CoursePO.of("計算機概論", 4);
        CoursePO course3 = CoursePO.of("會計學", 3);
        //courseRepository.saveAll(List.of(course1, course2, course3));

        // create students
        StudentPO student1 = StudentPO.of("Vincent");
        StudentPO student2 = StudentPO.of("Ivy");
        studentRepository.saveAll(List.of(student1, student2));

        // 使用辅助方法建立关系
        course1.addStudent(student1);
        course1.addStudent(student2);
        course2.addStudent(student1);
        course3.addStudent(student2);

        // 保存更新后的课程
        courseRepository.saveAll(List.of(course1, course2, course3));

        // 清除持久化上下文，确保从数据库重新加载
        entityManager.flush();
        entityManager.clear();

        // 查询学生并获取相关课程 - 使用带JOIN FETCH的查询
        StudentPO dbStudent1 = studentRepository.findById(student1.getId()).orElseThrow();
        assertEquals(Set.of(course1, course2), dbStudent1.getCourses());

        StudentPO dbStudent2 = studentRepository.findById(student2.getId()).orElseThrow();
        assertEquals(Set.of(course1, course3), dbStudent2.getCourses());

        // 查询课程并获取相关学生 - 使用带JOIN FETCH的查询
        CoursePO dbCourse1 = courseRepository.findById(course1.getId()).orElseThrow();
        assertEquals(Set.of(student1, student2), dbCourse1.getStudents());

        CoursePO dbCourse2 = courseRepository.findById(course2.getId()).orElseThrow();
        assertEquals(Set.of(student1), dbCourse2.getStudents());

        CoursePO dbCourse3 = courseRepository.findById(course3.getId()).orElseThrow();
        assertEquals(Set.of(student2), dbCourse3.getStudents());
    }
}
