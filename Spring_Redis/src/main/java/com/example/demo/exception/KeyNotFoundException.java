package com.example.demo.exception;

/**
 * 查詢的 key（或 Hash 欄位、ZSet 成員）不存在時拋出，由 GlobalExceptionHandler 轉成 404。
 */
public class KeyNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public KeyNotFoundException(String description) {
		super(description + " not found");
	}
}
