package com.example.demo.exception;

/**
 * 查不到指定 id 的書籍時拋出，由 GlobalExceptionHandler 轉成 404 ProblemDetail。
 * 繼承 RuntimeException（unchecked）：呼叫端不必宣告 throws，@Transactional 預設遇到 RuntimeException 也會 rollback。
 */
public class BookNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BookNotFoundException(Integer id) {
		super("Book not found: id=" + id);
	}
}
