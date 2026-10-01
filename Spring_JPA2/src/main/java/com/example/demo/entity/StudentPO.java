package com.example.demo.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

/**
 * 學生的 JPA Entity（PO = Persistent Object，與對外的 StudentDTO 區分）。
 * 與 {@link CoursePO} 為雙向多對多，本類別是「被擁有方（inverse side）」：
 * mappedBy = "students" 表示中間表由 CoursePO.students 維護，這一側的集合變動不會單獨寫入資料庫。
 */
@Entity
@Getter
@Setter
// 與 CoursePO 相同：排除對方集合，避免雙向關聯在 toString / equals / hashCode 中無限遞迴
@ToString(exclude = "courses")  // 排除courses欄位
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

    // mappedBy 指向擁有方 CoursePO 中的欄位名稱 students（是 Java 欄位名，不是資料表欄位名）
    @ManyToMany(
            mappedBy = "students",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}  // 新增級聯操作
    )
    private Set<CoursePO> courses = new HashSet<>();  // 初始化為可變集合

    // 新增關係維護方法
    // 因為本側是 inverse side，只改 courses 不會寫入中間表；
    // 必須同步更新 course.getStudents()（擁有方），資料庫才會真正新增關聯
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

    // 先從擁有方移除自己，再清空本側集合，避免走訪集合時同時修改它
    public void clearCourses() {
        courses.forEach(course -> course.getStudents().remove(this));
        courses.clear();
    }
}
