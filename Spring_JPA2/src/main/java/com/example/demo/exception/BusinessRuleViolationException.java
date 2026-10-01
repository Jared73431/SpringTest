package com.example.demo.exception;

/**
 * 請求本身正確，但與目前的資料狀態衝突，回 409。
 * 例如庫存不足、已出貨的訂單不能取消。
 * 繼承 RuntimeException（unchecked）：Service 不必宣告 throws，且 @Transactional 預設遇到 RuntimeException 會 rollback。
 */
public class BusinessRuleViolationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BusinessRuleViolationException(String message) {
		super(message);
	}
}
