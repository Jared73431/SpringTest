package com.example.demo.entity;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用者 Entity，與 {@link Todo} 為一對多。
 * 資料表命名為 tbl_user，因為 user 是 PostgreSQL 的保留字。
 */
@Entity
@Table(name = "tbl_user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    public String name;

    @Column(insertable = false, columnDefinition = "int default 1")
    private Integer gender = 1;

    // [Potential Bug] 目前 API 不會設定密碼，但 Controller 直接回傳 User Entity，JSON 仍會包含 password 欄位
    // （測試中已記錄此行為，暫不處理）。正式專案應以 BCrypt 等雜湊後再存，並透過 Response DTO 排除密碼
    @Column
    public String password;

    // inverse side：外鍵由 Todo.user 維護。
    // @JsonManagedReference：序列化時正常輸出 todos，Todo 端的 @JsonBackReference 則被略過，打斷循環參照。
    // cascade = ALL：儲存 / 刪除使用者時連同其 Todo 一起處理
    @JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    private Set<Todo> todos;
}
