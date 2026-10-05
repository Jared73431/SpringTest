package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 統一處理呼叫外部 API 時發生的錯誤，回傳 RFC 9457 ProblemDetail。RestClient 與 WebClient 的例外類別不同，但對應規則相同：
 *
 * <pre>
 * 外部 API 回 4xx（例如 404 查無資料） → 相同狀態碼：錯誤原因在請求本身，呼叫端需要知道
 * 外部 API 回 5xx                     → 502 Bad Gateway：問題出在外部服務，不是本服務
 * 連不上、逾時                         → 503 Service Unavailable
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// RestClient：外部 API 回 4xx / 5xx
	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<ProblemDetail> handleRestClientResponse(RestClientResponseException ex) {
		return upstreamError(ex.getStatusCode());
	}

	// WebClient：外部 API 回 4xx / 5xx
	@ExceptionHandler(WebClientResponseException.class)
	public ResponseEntity<ProblemDetail> handleWebClientResponse(WebClientResponseException ex) {
		return upstreamError(ex.getStatusCode());
	}

	// RestClient：連不上、逾時（I/O 錯誤）
	@ExceptionHandler(ResourceAccessException.class)
	public ProblemDetail handleRestClientUnavailable(ResourceAccessException ex) {
		return unavailable(ex);
	}

	// WebClient：連不上、逾時
	@ExceptionHandler(WebClientRequestException.class)
	public ProblemDetail handleWebClientUnavailable(WebClientRequestException ex) {
		return unavailable(ex);
	}

	private static ResponseEntity<ProblemDetail> upstreamError(HttpStatusCode status) {
		if (status.is4xxClientError()) {
			return ResponseEntity.status(status)
					.body(ProblemDetail.forStatusAndDetail(status, "JSONPlaceholder responded with status " + status.value()));
		}
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_GATEWAY, "JSONPlaceholder responded with status " + status.value()));
	}

	private static ProblemDetail unavailable(Exception ex) {
		// 回應只說明「無法使用」，詳細原因（連線被拒、逾時…）記錄在 log，方便排查
		log.warn("JSONPlaceholder is unavailable: {}", ex.getMessage(), ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "JSONPlaceholder is unavailable");
	}
}
