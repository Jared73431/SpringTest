package com.example.demo.client;

/**
 * 連不上外部 API 或逾時（OkHttp 拋出 IOException）時拋出。
 * 包成 unchecked 例外，呼叫端不必處處宣告 throws IOException，交給 GlobalExceptionHandler 統一處理。
 */
public class UpstreamUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UpstreamUnavailableException(Throwable cause) {
		super("JSONPlaceholder is unavailable: " + cause.getMessage(), cause);
	}
}
