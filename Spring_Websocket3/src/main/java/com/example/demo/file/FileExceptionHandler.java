package com.example.demo.file;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.example.demo.file.FileService.FileNotFoundException;

/**
 * 錯誤回應 ProblemDetail：400 內容不合法、404 沒有這個檔案、413 檔案太大。
 */
@RestControllerAdvice
public class FileExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail badRequest(IllegalArgumentException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(FileNotFoundException.class)
	public ProblemDetail notFound(FileNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "File is larger than 5MB");
	}
}
