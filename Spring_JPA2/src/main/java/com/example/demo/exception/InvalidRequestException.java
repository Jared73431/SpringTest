package com.example.demo.exception;

/**
 * 請求內容有問題，回 400。
 * 例如下單時 Request Body 中的商品 ID 不存在（URL 本身的資源是存在的）。
 * 與 ResourceNotFoundException 區分：Body 參照的資料不存在屬於「請求內容錯誤」，而非「URL 資源不存在」。
 */
public class InvalidRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidRequestException(String message) {
		super(message);
	}
}
