package com.example.demo.price;

/** 價格更新檔中的 ISBN 不存在：跳過這一行（skip），不讓整個 Job 失敗 */
public class UnknownIsbnException extends RuntimeException {

	public UnknownIsbnException(String isbn) {
		super("Unknown ISBN: " + isbn);
	}
}
