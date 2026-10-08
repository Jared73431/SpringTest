package com.example.demo.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 繼承 ResponseEntityExceptionHandler：Spring 的標準例外（驗證失敗、型別錯誤、缺少參數、ErrorResponseException…）
 * 都會轉成 ProblemDetail，並使用正確的狀態碼（400、404…）。沒有處理到的例外由 Spring Boot 回傳 500，
 * 不會把例外訊息回傳給客戶端。
 *
 * <p>
 * 修正前有 @ExceptionHandler(Exception.class)：所有例外都變成 500（id 不是數字、缺少必填參數本來應該是 400），
 * 而且把 ex.getMessage() 直接回傳，洩漏資料庫的 constraint 名稱等內部資訊。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// 驗證失敗：在 ProblemDetail 中加上每個欄位的錯誤訊息（保留修正前回應中的欄位錯誤）
	@Override
	protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(WebExchangeBindException ex,
			HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ex.getBody().setProperty("errors", errors);
		return super.handleWebExchangeBindException(ex, headers, status, exchange);
	}

	// 違反資料庫限制（CHECK、UNIQUE、外鍵）：詳細原因只寫進 log，回應不帶資料庫的訊息
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "資料違反資料庫的限制條件");
	}
}
