package com.example.demo.dto;

import java.math.BigDecimal;

/**
 * 搜尋條件：查詢參數直接綁定到這個 record（GET /api/cars/search?make=Toyota&yearFrom=2021…）。
 * 每個條件都是選填，有給的條件全部用 AND 組合。
 */
public record CarSearchCriteria(
		String make,
		String model,
		Integer year,
		BigDecimal minPrice,
		BigDecimal maxPrice,
		Integer yearFrom,
		Integer yearTo) {
}
