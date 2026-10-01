package com.example.demo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoRequest(
		@NotBlank(message = "待辦事項內容不可為空")
		@Size(max = 255, message = "待辦事項內容最多 255 字")
		String task) {
}
