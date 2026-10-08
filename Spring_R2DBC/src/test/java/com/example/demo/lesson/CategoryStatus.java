package com.example.demo.lesson;

import java.util.Arrays;

/**
 * 分類狀態：資料庫只存一個字元的代碼（CHAR(1)），程式中使用 enum（第 5 課）。
 */
public enum CategoryStatus {

	ACTIVE("A"), SUSPENDED("S");

	private final String code;

	CategoryStatus(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static CategoryStatus fromCode(String code) {
		return Arrays.stream(values())
				.filter(status -> status.code.equals(code))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("未知的分類狀態代碼：" + code));
	}
}
