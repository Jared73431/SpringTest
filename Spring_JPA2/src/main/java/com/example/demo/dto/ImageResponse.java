package com.example.demo.dto;

import java.util.Date;

import com.example.demo.entity.Image;
import com.example.demo.repository.ImageSummary;

/**
 * 圖片的摘要資訊（不含圖片內容），圖片內容另外以 GET /api/images/{id} 取得。
 * 使用 Java record：不可變、自動產生建構子 / accessor / equals / hashCode，適合單純的回應 DTO。
 * 提供兩個靜態工廠 from()，分別由查詢投影（列表）與完整 Entity（剛上傳後）轉換。
 */
public record ImageResponse(
		Long id,
		String name,
		String contentType,
		Integer size,
		Date uploadDate) {

	public static ImageResponse from(ImageSummary summary) {
		return new ImageResponse(summary.getId(), summary.getName(), summary.getContentType(), summary.getSize(),
				summary.getUploadDate());
	}

	// 此版本會讀取 data 計算大小，只適合已在記憶體中的單一 Entity；列表請改用 ImageSummary 版本
	public static ImageResponse from(Image image) {
		return new ImageResponse(image.getId(), image.getName(), image.getContentType(), image.getData().length,
				image.getUploadDate());
	}
}
