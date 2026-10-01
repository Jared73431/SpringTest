package com.example.demo.entity;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 待辦事項 Entity，與 {@link User} 為多對一（每個 Todo 屬於一位使用者）。
 * 建立 / 修改時間由 Spring Data JPA Auditing 自動填入，需搭配 JpaAuditingConfig 的 @EnableJpaAuditing。
 */
@Entity
@Table
// 註冊 AuditingEntityListener：在 persist / update 前自動填入 @CreatedDate、@LastModifiedDate 欄位
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String task;

    // insertable = false：INSERT 語句不包含此欄位，改由資料庫的 default 1 決定初始值；
    // 注意 Java 端的初始值 1 不會被寫入，只是讓新物件在記憶體中也有一致的值
    @Column(insertable = false, columnDefinition = "int default 1")
    private Integer status = 1;

    // updatable = false：建立時間只在 INSERT 時寫入，之後 UPDATE 不會覆蓋
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Date createTime;

    @LastModifiedDate
    @Column(nullable = false)
    private Date updateTime;

    // 擁有方：外鍵 user_id 在 todo 表。
    // @JsonBackReference：序列化 JSON 時略過此欄位，與 User.todos 的 @JsonManagedReference 成對，避免 User ↔ Todo 無限遞迴。
    // 刻意不設 cascade：刪除 Todo 不應連帶刪除 User
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
}
