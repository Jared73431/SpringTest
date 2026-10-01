package com.example.demo.exception;

/**
 * URL 指定的資源不存在，回 404。
 * 例如 GET /api/orders/{id} 查不到訂單。
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String resource, Object id) {
		super(resource + "不存在: " + id);
	}
}
