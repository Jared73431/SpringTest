package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 繼承 ErrorResponseException：Spring MVC 會直接轉成 404 ProblemDetail */
public class UserNotFoundException extends ErrorResponseException {

	public UserNotFoundException(Long id) {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "找不到使用者：" + id), null);
	}
}
