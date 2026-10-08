package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.Car;

/**
 * API 回應（JSON 欄位與修正前直接回傳 Entity 時相同）。Entity 不直接當成回應，資料表的變更才不會直接影響 API。
 */
public record CarResponse(
		Long id,
		String make,
		String model,
		Integer year,
		String color,
		BigDecimal price,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static CarResponse from(Car car) {
		return new CarResponse(car.getId(), car.getMake(), car.getModel(), car.getYear(), car.getColor(),
				car.getPrice(), car.getCreatedAt(), car.getUpdatedAt());
	}
}
