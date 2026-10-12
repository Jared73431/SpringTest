package com.example.demo.reminder;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 修正前名稱寫死成 myJob：第二次新增失敗，例外被 printStackTrace 吞掉，API 仍然回傳 ok */
public class DuplicateReminderException extends ErrorResponseException {

	public DuplicateReminderException(String name) {
		super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "提醒已存在：" + name), null);
	}
}
