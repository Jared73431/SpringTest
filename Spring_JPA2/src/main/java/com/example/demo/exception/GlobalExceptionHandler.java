package com.example.demo.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
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
 * 統一處理 Controller 拋出的例外，回傳 RFC 9457 ProblemDetail 格式的錯誤回應。
 * 繼承 ResponseEntityExceptionHandler：Spring 內建的錯誤（400 格式錯誤、405 等）也由這裡以 ProblemDetail 回應。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleViolationException.class)
	public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	// @Version 樂觀鎖衝突：資料在讀取後已被其他交易修改，請用戶端重新操作
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "資料已被其他交易修改，請重新操作");
	}

	/**
	 * Bean Validation（@Valid）失敗：在 ProblemDetail 加上 errors 欄位，列出每個欄位的錯誤訊息，
	 * 例如 {"errors": {"name": "商品名稱不可為空"}}
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

		ProblemDetail problem = ex.getBody();
		problem.setDetail("請求內容驗證失敗");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}
}
