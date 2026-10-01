package com.example.demo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 建立待辦事項的 Request，使用 record 定義不可變的請求物件；
 * Bean Validation 註解直接寫在 record 元件上，搭配 Controller 的 @Valid 生效。
 * 只接收 task，狀態與時間戳記由資料庫預設值與 JPA Auditing 決定。
 */
public record CreateTodoRequest(
		@NotBlank(message = "待辦事項內容不可為空")
		@Size(max = 255, message = "待辦事項內容最多 255 字")
		String task) {
}
