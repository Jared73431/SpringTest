package com.example.demo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 建立使用者的 Request（record）。只開放 name，避免用戶端直接傳入 id 或其他欄位
 * 而被綁定到 User Entity 上（Mass Assignment 問題）。
 */
public record CreateUserRequest(
		@NotBlank(message = "使用者名稱不可為空")
		@Size(max = 255, message = "使用者名稱最多 255 字")
		String name) {
}
