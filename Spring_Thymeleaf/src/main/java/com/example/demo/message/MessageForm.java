package com.example.demo.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 表單綁定用的物件（form backing object）。
 *
 * <p>
 * 用一般的 class 而不是 record：Thymeleaf 的 th:field 透過 getter 讀取欄位值，驗證失敗回到頁面時才能把使用者剛輸入的內容填回去；
 * Spring 綁定表單時也透過 setter 寫入。
 */
public class MessageForm {

	@NotBlank(message = "請輸入名稱")
	@Size(max = 30, message = "名稱最多 30 個字")
	private String author;

	@NotBlank(message = "請輸入留言")
	@Size(max = 200, message = "留言最多 200 個字")
	private String content;

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
