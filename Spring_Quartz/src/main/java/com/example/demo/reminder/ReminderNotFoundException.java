package com.example.demo.reminder;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ReminderNotFoundException extends ErrorResponseException {

	public ReminderNotFoundException(String name) {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "找不到提醒：" + name), null);
	}
}
