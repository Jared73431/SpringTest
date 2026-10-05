package com.example.demo.client;

/**
 * Book Provider 回傳錯誤（非 2xx）時拋出，保留 Provider 的狀態碼、Content-Type 與回應內容。
 */
public class BookProviderException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	private final String contentType;

	private final byte[] body;

	public BookProviderException(int status, String contentType, byte[] body) {
		super("Book provider responded with status " + status);
		this.status = status;
		this.contentType = contentType;
		this.body = body;
	}

	public int getStatus() {
		return status;
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getBody() {
		return body;
	}
}
