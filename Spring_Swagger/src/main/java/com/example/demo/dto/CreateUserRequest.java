package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用戶創建請求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "用戶名不能為空")
    @Schema(description = "用戶名", example = "john_doe", required = true)
    private String username;

    @NotBlank(message = "姓名不能為空")
    @Schema(description = "真實姓名", example = "John Doe", required = true)
    private String name;

    @Email(message = "請提供有效的郵箱地址")
    @NotBlank(message = "郵箱不能為空")
    @Schema(description = "郵箱地址", example = "john@example.com", required = true)
    private String email;

    @NotNull(message = "年齡不能為空")
    @Schema(description = "年齡", example = "25", required = true)
    private Integer age;
}
