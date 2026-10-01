package com.example.demo.client;

/**
 * Book Server 回傳錯誤（非 2xx）時拋出，保留 Server 的狀態碼、Content-Type 與回應內容。
 */
public class BookServerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	private final String contentType;

	private final byte[] body;

	public BookServerException(int status, String contentType, byte[] body) {
		super("Book server responded with status " + status);
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
