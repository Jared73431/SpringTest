package com.example.demo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
		@NotBlank(message = "使用者名稱不可為空")
		@Size(max = 255, message = "使用者名稱最多 255 字")
		String name) {
}
