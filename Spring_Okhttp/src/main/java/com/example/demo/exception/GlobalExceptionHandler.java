package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.client.UpstreamResponseException;
import com.example.demo.client.UpstreamUnavailableException;

/**
 * 統一處理呼叫外部 API 時發生的錯誤，回傳 RFC 9457 ProblemDetail（規則與 Spring_HttpClient 相同）：
 *
 * <pre>
 * 外部 API 回 4xx（例如 404 查無資料） → 相同狀態碼
 * 外部 API 回 5xx                     → 502 Bad Gateway
 * 連不上、逾時                         → 503 Service Unavailable
 * </pre>
 *
 * 修正前 Service 把所有錯誤都吃掉、只印在 console，API 永遠回 200「執行完成」。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(UpstreamResponseException.class)
	public ResponseEntity<ProblemDetail> handleUpstreamResponse(UpstreamResponseException ex) {
		HttpStatusCode status = HttpStatusCode.valueOf(ex.getStatus());
		if (status.is4xxClientError()) {
			return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, ex.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage()));
	}

	@ExceptionHandler(UpstreamUnavailableException.class)
	public ProblemDetail handleUpstreamUnavailable(UpstreamUnavailableException ex) {
		// 回應只說明「無法使用」，詳細原因（連線被拒、逾時…）記錄在 log，方便排查
		log.warn(ex.getMessage(), ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "JSONPlaceholder is unavailable");
	}
}
