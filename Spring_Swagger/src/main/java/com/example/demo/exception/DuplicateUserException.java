package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 帳號或電子郵件已被使用：409 Conflict（修正前違反 unique 限制時，由 catch-all 回傳 500） */
public class DuplicateUserException extends ErrorResponseException {

	public DuplicateUserException(String field, String value) {
		super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, field + " 已被使用：" + value),
				null);
	}
}
