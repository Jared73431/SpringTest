package com.example.demo.tests;

import org.springframework.context.annotation.Import;
import com.example.demo.TestcontainersConfiguration;

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
@Import(TestcontainersConfiguration.class)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)// 新增交易注解，確保在測試期間會話保持打開
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

        // 使用輔助方法建立關係
        course1.addStudent(student1);
        course1.addStudent(student2);
        course2.addStudent(student1);
        course3.addStudent(student2);

        // 保存更新後的課程
        courseRepository.saveAll(List.of(course1, course2, course3));

        // 清除持久化上下文，確保從資料庫重新加載
        entityManager.flush();
        entityManager.clear();

        // 查詢學生並取得相關課程 - 使用帶JOIN FETCH的查詢
        StudentPO dbStudent1 = studentRepository.findById(student1.getId()).orElseThrow();
        assertEquals(Set.of(course1, course2), dbStudent1.getCourses());

        StudentPO dbStudent2 = studentRepository.findById(student2.getId()).orElseThrow();
        assertEquals(Set.of(course1, course3), dbStudent2.getCourses());

        // 查詢課程並取得相關學生 - 使用帶JOIN FETCH的查詢
        CoursePO dbCourse1 = courseRepository.findById(course1.getId()).orElseThrow();
        assertEquals(Set.of(student1, student2), dbCourse1.getStudents());

        CoursePO dbCourse2 = courseRepository.findById(course2.getId()).orElseThrow();
        assertEquals(Set.of(student1), dbCourse2.getStudents());

        CoursePO dbCourse3 = courseRepository.findById(course3.getId()).orElseThrow();
        assertEquals(Set.of(student2), dbCourse3.getStudents());
    }
}
