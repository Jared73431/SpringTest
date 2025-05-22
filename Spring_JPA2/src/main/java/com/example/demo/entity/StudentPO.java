package com.example.demo.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString(exclude = "courses")  // 排除courses字段
@EqualsAndHashCode(exclude = "courses")
@Table(name = "student")
public class StudentPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private long id;

    private String name;

    public static StudentPO of(String name) {
        var student = new StudentPO();
        student.name = name;

        return student;
    }

    @ManyToMany(
            mappedBy = "students",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}  // 添加级联操作
    )
    private Set<CoursePO> courses = new HashSet<>();  // 初始化为可变集合

    // 添加关系维护方法
    public void addCourse(CoursePO course) {
        if (course != null) {
            courses.add(course);
            if (!course.getStudents().contains(this)) {
                course.getStudents().add(this);
            }
        }
    }

    public void removeCourse(CoursePO course) {
        if (course != null) {
            courses.remove(course);
            if (course.getStudents().contains(this)) {
                course.getStudents().remove(this);
            }
        }
    }

    // Batch operations for multiple courses
    public void addCourses(Collection<CoursePO> coursesToAdd) {
        if (coursesToAdd != null) {
            coursesToAdd.forEach(this::addCourse);
        }
    }

    public void removeCourses(Collection<CoursePO> coursesToRemove) {
        if (coursesToRemove != null) {
            coursesToRemove.forEach(this::removeCourse);
        }
    }

    public void clearCourses() {
        courses.forEach(course -> course.getStudents().remove(this));
        courses.clear();
    }
}
