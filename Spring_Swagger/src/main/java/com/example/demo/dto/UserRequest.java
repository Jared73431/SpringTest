package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改使用者的請求（修正前是 Lombok 的 CreateUserRequest）。驗證條件與資料表一致。
 *
 * <p>
 * 減少註解雜訊：必填、長度、Email 格式、數值範圍，springdoc 都會從 Bean Validation 自動推導進 OpenAPI 文件，
 * 不需要修正前的 @Schema(required = true, ...)。@Schema 只用來補上 Swagger UI「Try it out」的範例值
 * （沒有範例時，字串欄位只會顯示 "string"）。
 */
public record UserRequest(
		@Schema(example = "john_doe")
		@NotBlank(message = "帳號不可空白")
		@Size(max = 50, message = "帳號最多 50 個字")
		String username,

		@Schema(example = "John Doe")
		@NotBlank(message = "姓名不可空白")
		@Size(max = 100, message = "姓名最多 100 個字")
		String name,

		@Schema(example = "john@example.com")
		@NotBlank(message = "電子郵件不可空白")
		@Email(message = "電子郵件格式不正確")
		@Size(max = 255, message = "電子郵件最多 255 個字")
		String email,

		@Schema(example = "25")
		@NotNull(message = "年齡不可空白")
		@Min(value = 0, message = "年齡不可小於 0")
		@Max(value = 150, message = "年齡不可大於 150")
		Integer age) {
}
