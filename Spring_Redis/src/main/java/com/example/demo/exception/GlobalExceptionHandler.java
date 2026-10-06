package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 統一處理 Redis 相關錯誤，回傳 RFC 9457 ProblemDetail：
 *
 * <pre>
 * key 不存在                                  → 404 Not Found
 * 對 key 使用錯誤的資料結構操作（WRONGTYPE）   → 409 Conflict，例如對 String 的 key 讀取 Hash
 * 連不上 Redis                                → 503 Service Unavailable
 * </pre>
 * Request Body 驗證失敗（400）由 Spring MVC 處理（spring.mvc.problemdetails.enabled=true）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(KeyNotFoundException.class)
	public ProblemDetail handleKeyNotFound(KeyNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	// Redis 回傳的錯誤（由 Lettuce 拋出、Spring 轉換成 RedisSystemException）
	@ExceptionHandler(RedisSystemException.class)
	public ProblemDetail handleRedisError(RedisSystemException ex) {
		String message = ex.getMostSpecificCause().getMessage();
		if (message != null && message.startsWith("WRONGTYPE")) {
			return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
					"The key holds a different data type: " + message);
		}
		throw ex;
	}

	@ExceptionHandler(RedisConnectionFailureException.class)
	public ProblemDetail handleRedisUnavailable(RedisConnectionFailureException ex) {
		log.warn("Redis is unavailable: {}", ex.getMessage(), ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Redis is unavailable");
	}
}
