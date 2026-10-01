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
 * 狀態碼慣例：404 = URL 指定的資源不存在；400 = 請求內容不合法；409 = 請求合法但與目前資料狀態衝突。
 * 集中處理後 Controller 不需各自 try/catch，所有 API 的錯誤格式也能保持一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	// 404：URL 中的 id 查不到資料
	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	// 400：Request Body / 參數有問題，例如參照了不存在的商品 ID
	@ExceptionHandler(InvalidRequestException.class)
	public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	// 409：違反商業規則，例如庫存不足、不合法的訂單狀態轉換
	@ExceptionHandler(BusinessRuleViolationException.class)
	public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	// @Version 樂觀鎖衝突：資料在讀取後已被其他交易修改，請用戶端重新操作
	// 衝突通常在交易 commit（flush）時才被偵測到，不是 Service 主動拋出，因此在這裡統一轉成 409
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
		// LinkedHashMap 保留欄位順序；同一欄位有多個錯誤時 putIfAbsent 只保留第一個
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

		// 沿用 Spring 已建立的 ProblemDetail（status、title 已填好），只補上 detail 與 errors
		ProblemDetail problem = ex.getBody();
		problem.setDetail("請求內容驗證失敗");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}
}
