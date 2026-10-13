package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 統一處理呼叫 Server 時發生的錯誤，回傳 RFC 9457 ProblemDetail（規則和 Spring_Feign_Client 相同）。
 *
 * <pre>
 * Server 回 4xx（404 查無資料、400 驗證失敗…） → 原樣轉回：狀態碼與 ProblemDetail 不變，呼叫端看得到真正的原因
 * Server 回 5xx                                → 502 Bad Gateway：問題出在下游服務，不是 Client 本身
 * 連不上 Server（拒絕連線、逾時）               → 503 Service Unavailable
 * </pre>
 *
 * 和 Feign 的差別：RestClient 遇到 4xx、5xx 預設就拋出 RestClientResponseException，裡面已經有狀態碼、標頭與回應內容，
 * 不需要像 Feign 一樣自己寫 ErrorDecoder 和自訂的例外。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<?> handleBookServerError(RestClientResponseException ex) {
		HttpStatusCode status = ex.getStatusCode();
		if (!status.is4xxClientError()) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ProblemDetail
					.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Book server error: status " + status.value()));
		}
		byte[] body = ex.getResponseBodyAsByteArray();
		if (body.length == 0) {
			return ResponseEntity.status(status).body(ProblemDetail.forStatus(status));
		}
		MediaType contentType = ex.getResponseHeaders() != null && ex.getResponseHeaders().getContentType() != null
				? ex.getResponseHeaders().getContentType()
				: MediaType.APPLICATION_PROBLEM_JSON;
		return ResponseEntity.status(status).contentType(contentType).body(body);
	}

	// 拒絕連線、讀取逾時等 I/O 錯誤：RestClient 拋出 ResourceAccessException（Feign 是 RetryableException）
	@ExceptionHandler(ResourceAccessException.class)
	public ProblemDetail handleServerUnavailable(ResourceAccessException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Book server is unavailable");
	}
}
