package com.example.demo.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * 允許上傳的圖片格式，依「檔案開頭的位元組」（magic number）判斷。
 *
 * <p>
 * 為什麼不看副檔名或 Content-Type：兩者都由上傳的人決定，可以偽裝。把 HTML 取名為 cat.png、宣告成 image/png 上傳，
 * 之後如果用它的 Content-Type 顯示，瀏覽器會在本網站的網域下執行裡面的 script（Stored XSS）。
 *
 * <p>
 * 不接受 SVG：SVG 是 XML 文字，可以包含 &lt;script&gt;。
 */
public enum ImageFormat {

	PNG("image/png", new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n' }),
	JPEG("image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
	GIF("image/gif", new byte[] { 'G', 'I', 'F', '8' }),
	// WebP：RIFF....WEBP（第 4～7 個位元組是檔案大小）
	WEBP("image/webp", new byte[] { 'R', 'I', 'F', 'F' });

	private final String contentType;

	private final byte[] signature;

	ImageFormat(String contentType, byte[] signature) {
		this.contentType = contentType;
		this.signature = signature;
	}

	public String contentType() {
		return contentType;
	}

	/** 從檔案內容判斷格式；不是允許的圖片回傳 empty */
	public static Optional<ImageFormat> detect(byte[] data) {
		return Arrays.stream(values()).filter(format -> format.matches(data)).findFirst();
	}

	/** 資料庫裡存的 Content-Type 是否為允許的圖片（修正前上傳的檔案可能不是） */
	public static boolean isAllowed(String contentType) {
		return Arrays.stream(values()).anyMatch(format -> format.contentType.equalsIgnoreCase(contentType));
	}

	private boolean matches(byte[] data) {
		if (data.length < signature.length || !Arrays.equals(data, 0, signature.length, signature, 0, signature.length)) {
			return false;
		}
		return this != WEBP || (data.length >= 12 && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P');
	}
}
