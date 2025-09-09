package com.example.demo.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "users")
@Schema(description = "用戶實體")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "用戶ID", example = "1")
    private Long id;

    @NotBlank(message = "用戶名不能為空")
    @Column(unique = true)
    @Schema(description = "用戶名", example = "john_doe", required = true)
    private String username;

    @NotBlank(message = "姓名不能為空")
    @Schema(description = "真實姓名", example = "John Doe", required = true)
    private String name;

    @Email(message = "請提供有效的郵箱地址")
    @NotBlank(message = "郵箱不能為空")
    @Column(unique = true)
    @Schema(description = "郵箱地址", example = "john@example.com", required = true)
    private String email;

    @NotNull(message = "年齡不能為空")
    @Schema(description = "年齡", example = "25", required = true)
    private Integer age;

    public User(@NotBlank(message = "用戶名不能為空") String username, @NotBlank(message = "姓名不能為空") String name, @Email(message = "請提供有效的郵箱地址") @NotBlank(message = "郵箱不能為空") String email, @NotNull(message = "年齡不能為空") Integer age) {
    }
}
