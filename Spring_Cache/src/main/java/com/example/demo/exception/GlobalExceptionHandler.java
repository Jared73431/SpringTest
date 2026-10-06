package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 統一的錯誤回應（RFC 9457 ProblemDetail）。Request Body 驗證失敗（400）由 Spring MVC 處理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(UserNotFoundException.class)
	public ProblemDetail handleNotFound(UserNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	// 快取伺服器（Redis）連不上時
	@ExceptionHandler(RedisConnectionFailureException.class)
	public ProblemDetail handleRedisUnavailable(RedisConnectionFailureException ex) {
		log.warn("Redis is unavailable: {}", ex.getMessage(), ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Cache server is unavailable");
	}
}
