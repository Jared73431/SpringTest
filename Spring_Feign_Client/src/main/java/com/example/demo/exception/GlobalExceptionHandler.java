package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.client.BookServerException;

import feign.RetryableException;

/**
 * 統一處理呼叫 Server 時發生的錯誤，回傳 RFC 9457 ProblemDetail 格式的錯誤回應。
 *
 * <pre>
 * Server 回 4xx（404 查無資料、400 驗證失敗…） → 原樣轉回：狀態碼與 ProblemDetail 不變，呼叫端看得到真正的原因
 * Server 回 5xx                                → 502 Bad Gateway：問題出在下游服務，不是 Client 本身
 * 連不上 Server（拒絕連線、逾時）               → 503 Service Unavailable
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BookServerException.class)
	public ResponseEntity<?> handleBookServerError(BookServerException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getStatus());
		if (status == null || !status.is4xxClientError()) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ProblemDetail
					.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Book server error: status " + ex.getStatus()));
		}
		if (ex.getBody().length == 0) {
			return ResponseEntity.status(status).body(ProblemDetail.forStatus(status));
		}
		MediaType contentType = ex.getContentType() != null ? MediaType.parseMediaType(ex.getContentType())
				: MediaType.APPLICATION_PROBLEM_JSON;
		return ResponseEntity.status(status).contentType(contentType).body(ex.getBody());
	}

	// Feign 遇到 IOException（拒絕連線、讀取逾時等）時拋出 RetryableException
	@ExceptionHandler(RetryableException.class)
	public ProblemDetail handleServerUnavailable(RetryableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Book server is unavailable");
	}
}
