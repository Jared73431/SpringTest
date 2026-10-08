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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 繼承 ResponseEntityExceptionHandler：Spring 的標準例外（驗證失敗、型別錯誤、JSON 格式錯誤、ErrorResponseException…）
 * 都會轉成 ProblemDetail 並使用正確的狀態碼。沒有處理到的例外由 Spring Boot 回傳 500，不帶例外訊息。
 *
 * <p>
 * 修正前：
 * <ul>
 * <li>驗證失敗時把欄位錯誤收集到 errors，卻沒有放進回應</li>
 * <li>@ExceptionHandler(Exception.class) 把所有例外都變成 500（id 不是數字本來是 400），而且<b>沒有寫 log</b>，錯誤原因完全看不到</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ex.getBody().setProperty("errors", errors);
		return super.handleMethodArgumentNotValid(ex, headers, status, request);
	}

	// 兩個請求同時用同一個帳號註冊時，事先的重複檢查都會通過，最後由資料庫的 UNIQUE 擋下：同樣回傳 409
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "資料違反資料庫的限制條件");
	}
}
