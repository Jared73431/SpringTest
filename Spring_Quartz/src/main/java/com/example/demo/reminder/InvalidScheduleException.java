package com.example.demo.reminder;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidScheduleException extends ErrorResponseException {

	public InvalidScheduleException(String detail) {
		super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
	}
}
