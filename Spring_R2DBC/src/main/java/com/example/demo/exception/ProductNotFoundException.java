package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * 繼承 ErrorResponseException：WebFlux 會直接把它轉成 ProblemDetail 回應（404），不需要另外寫 @ExceptionHandler。
 */
public class ProductNotFoundException extends ErrorResponseException {

	public ProductNotFoundException(Integer id) {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "找不到商品：" + id), null);
	}
}
