package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改汽車的請求（修正前是 Lombok 的 CarDto，另外有一個沒有用到的 id 欄位）。
 * 驗證條件與資料表一致：VARCHAR(50) / VARCHAR(30)、CHECK (year &gt; 1900 AND year &lt;= 2100)、DECIMAL(10, 2)。
 */
public record CarRequest(
		@NotBlank(message = "Make is required")
		@Size(max = 50, message = "Make must be at most 50 characters")
		String make,

		@NotBlank(message = "Model is required")
		@Size(max = 50, message = "Model must be at most 50 characters")
		String model,

		@NotNull(message = "Year is required")
		@Min(value = 1901, message = "Year must be greater than 1900")
		@Max(value = 2100, message = "Year must be at most 2100")
		Integer year,

		@Size(max = 30, message = "Color must be at most 30 characters")
		String color,

		@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
		@Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimals")
		BigDecimal price) {
}
