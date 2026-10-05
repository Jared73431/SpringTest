package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.client.BookProviderException;

import feign.RetryableException;

/**
 * 統一處理呼叫 Provider 時發生的錯誤，回傳 RFC 9457 ProblemDetail 格式的錯誤回應。
 *
 * <pre>
 * Provider 回 4xx（404 查無資料、400 驗證失敗…） → 原樣轉回：狀態碼與 ProblemDetail 不變，呼叫端看得到真正的原因
 * 沒有可用的 Provider 實例（LoadBalancer 回 503） → 503 Service Unavailable
 * 連不上 Provider（拒絕連線、逾時）                → 503 Service Unavailable
 * Provider 回其他 5xx                              → 502 Bad Gateway：問題出在下游服務，不是 Client 本身
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BookProviderException.class)
	public ResponseEntity<?> handleBookProviderError(BookProviderException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getStatus());
		if (status == HttpStatus.SERVICE_UNAVAILABLE) {
			return ResponseEntity.status(status).body(unavailable());
		}
		if (status == null || !status.is4xxClientError()) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ProblemDetail
					.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Book provider error: status " + ex.getStatus()));
		}
		if (ex.getBody().length == 0) {
			return ResponseEntity.status(status).body(ProblemDetail.forStatus(status));
		}
		MediaType contentType = ex.getContentType() != null ? MediaType.parseMediaType(ex.getContentType())
				: MediaType.APPLICATION_PROBLEM_JSON;
		return ResponseEntity.status(status).contentType(contentType).body(ex.getBody());
	}

	// Feign 遇到 IOException（拒絕連線、讀取逾時等）時拋出 RetryableException
	// 例如 Provider 已經停止，但 Eureka 還沒把它從清單移除時
	@ExceptionHandler(RetryableException.class)
	public ProblemDetail handleProviderUnreachable(RetryableException ex) {
		return unavailable();
	}

	private static ProblemDetail unavailable() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Book provider is unavailable");
	}
}
