package com.example.demo.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

/**
 * 課程的 JPA Entity（PO = Persistent Object，對應資料表的持久化物件，與對外回傳的 DTO 區分）。
 * 與 {@link StudentPO} 為雙向多對多，本類別是關聯的「擁有方（owning side）」：
 * 由這裡的 @JoinTable 決定中間表 selected_course 的寫入。
 */
@Entity
@Getter
@Setter
// 雙向關聯時，若 toString / equals / hashCode 包含對方的集合，
// 兩邊會互相呼叫造成無限遞迴（StackOverflowError），也可能意外觸發 LAZY 集合載入，因此排除 students
@ToString(exclude = "students")  // 排除students欄位
@EqualsAndHashCode(exclude = "students")  // 排除students欄位
@Table(name = "course")
public class CoursePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private long id;

    private String name;

    private int point;

    /** 靜態工廠方法：以語意化的名稱建立尚未持久化的課程（id 由資料庫 IDENTITY 產生） */
    public static CoursePO of(String name, int point) {
        var course = new CoursePO();
        course.name = name;
        course.point = point;

        return course;
    }

    // 擁有方：沒有 mappedBy，中間表的新增 / 刪除以「這一側」的集合內容為準。
    // LAZY：查詢課程時不會立即載入所有學生，存取集合時才發 SQL（需在交易 / Session 內）。
    // 只級聯 PERSIST / MERGE，不使用 REMOVE：多對多時刪除課程不應連帶刪除學生本身。
    @ManyToMany(
            targetEntity = StudentPO.class,
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}  // 新增級聯操作
    )
    // @JoinTable 定義中間表：joinColumns 指向本方（course），inverseJoinColumns 指向對方（student）
    @JoinTable(
            name = "selected_course",
            joinColumns = @JoinColumn(name = "course", referencedColumnName = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "student", referencedColumnName = "student_id")
    )
    private Set<StudentPO> students = new HashSet<>();  // 初始化為可變集合

    // 新增關係維護方法
    // 雙向關聯必須同時更新兩側的集合：JPA 只依擁有方寫入資料庫，
    // 但若只改一側，同一個交易內記憶體中的物件狀態會與資料庫不一致。
    // 先檢查 contains 再加入，避免與 StudentPO.addCourse 互相呼叫時重複處理。
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

    // 先從每位學生那側移除自己，再清空本側集合；
    // 若直接在 forEach 中呼叫 removeStudent 會邊走訪邊修改 students，造成 ConcurrentModificationException
    public void clearStudents() {
        students.forEach(student -> student.getCourses().remove(this));
        students.clear();
    }

}
