package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改使用者的請求（修正前是 Lombok 的 CreateUserRequest）。驗證條件與資料表一致。
 */
public record UserRequest(
		@NotBlank(message = "帳號不可空白")
		@Size(max = 50, message = "帳號最多 50 個字")
		String username,

		@NotBlank(message = "姓名不可空白")
		@Size(max = 100, message = "姓名最多 100 個字")
		String name,

		@NotBlank(message = "電子郵件不可空白")
		@Email(message = "電子郵件格式不正確")
		@Size(max = 255, message = "電子郵件最多 255 個字")
		String email,

		@NotNull(message = "年齡不可空白")
		@Min(value = 0, message = "年齡不可小於 0")
		@Max(value = 150, message = "年齡不可大於 150")
		Integer age) {
}
