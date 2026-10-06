package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 統一的錯誤回應（RFC 9457 ProblemDetail）。Request Body 驗證失敗（400）由 Spring MVC 處理。
 * 與 Spring_Cache 不同：快取在本機記憶體，沒有「快取伺服器連不上」的情況。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UserNotFoundException.class)
	public ProblemDetail handleNotFound(UserNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}
}
