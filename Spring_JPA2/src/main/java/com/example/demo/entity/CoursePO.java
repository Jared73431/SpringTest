package com.example.demo.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString(exclude = "students")  // 排除students字段
@EqualsAndHashCode(exclude = "students")  // 排除students字段
@Table(name = "course")
public class CoursePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private long id;

    private String name;

    private int point;

    public static CoursePO of(String name, int point) {
        var course = new CoursePO();
        course.name = name;
        course.point = point;

        return course;
    }

    @ManyToMany(
            targetEntity = StudentPO.class,
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}  // 添加级联操作
    )
    @JoinTable(
            name = "selected_course",
            joinColumns = @JoinColumn(name = "course", referencedColumnName = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "student", referencedColumnName = "student_id")
    )
    private Set<StudentPO> students = new HashSet<>();  // 初始化为可变集合

    // 添加关系维护方法
    public void addStudent(StudentPO student) {
        if (student != null) {
            students.add(student);
            if (!student.getCourses().contains(this)) {
                student.getCourses().add(this);
            }
        }
    }

    public void removeStudent(StudentPO student) {
        if (student != null) {
            students.remove(student);
            if (student.getCourses().contains(this)) {
                student.getCourses().remove(this);
            }
        }
    }

    // Batch operations for multiple students
    public void addStudents(Collection<StudentPO> studentsToAdd) {
        if (studentsToAdd != null) {
            studentsToAdd.forEach(this::addStudent);
        }
    }

    public void removeStudents(Collection<StudentPO> studentsToRemove) {
        if (studentsToRemove != null) {
            studentsToRemove.forEach(this::removeStudent);
        }
    }

    public void clearStudents() {
        students.forEach(student -> student.getCourses().remove(this));
        students.clear();
    }

}
