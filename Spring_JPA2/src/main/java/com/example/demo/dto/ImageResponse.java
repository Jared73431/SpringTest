package com.example.demo.dto;

import java.util.Date;

import com.example.demo.entity.Image;
import com.example.demo.repository.ImageSummary;

/**
 * 圖片的摘要資訊（不含圖片內容），圖片內容另外以 GET /api/images/{id} 取得。
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

	public static ImageResponse from(Image image) {
		return new ImageResponse(image.getId(), image.getName(), image.getContentType(), image.getData().length,
				image.getUploadDate());
	}
}
