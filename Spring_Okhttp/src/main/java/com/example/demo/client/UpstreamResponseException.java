package com.example.demo.client;

/**
 * 外部 API 回傳非 2xx 時拋出。
 * OkHttp 不會因為 4xx / 5xx 拋出例外（只有網路錯誤才會），必須自己檢查 response.isSuccessful()。
 */
public class UpstreamResponseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public UpstreamResponseException(int status) {
		super("JSONPlaceholder responded with status " + status);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
