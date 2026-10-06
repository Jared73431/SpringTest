package com.example.demo.exception;

/**
 * 查不到使用者時拋出，由 GlobalExceptionHandler 轉成 404。
 * 例外發生時，方法上的 @CachePut / @CacheEvict 都不會執行，快取維持原狀。
 */
public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UserNotFoundException(Object key) {
		super("User not found: " + key);
	}
}
