package com.example.demo.exception;

/**
 * 請求本身正確，但與目前的資料狀態衝突，回 409。
 * 例如庫存不足、已出貨的訂單不能取消。
 */
public class BusinessRuleViolationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BusinessRuleViolationException(String message) {
		super(message);
	}
}
