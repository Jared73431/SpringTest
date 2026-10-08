package com.example.demo.lesson;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

/**
 * 第 5 課：自訂型別轉換。沒有自訂 Converter 時，enum 會以名稱（"ACTIVE"）存入資料庫；
 * 這裡改成存一個字元的代碼（"A"），讀取時再轉回 enum。
 *
 * <p>
 * 放在測試程式碼的 com.example.demo 底下，執行測試時會被元件掃描載入（正式程式碼沒有用到 category）。
 */
@Configuration(proxyBeanMethods = false)
class LessonR2dbcConfiguration {

	@WritingConverter
	static class CategoryStatusWritingConverter implements Converter<CategoryStatus, String> {
		@Override
		public String convert(CategoryStatus status) {
			return status.code();
		}
	}

	@ReadingConverter
	static class CategoryStatusReadingConverter implements Converter<String, CategoryStatus> {
		@Override
		public CategoryStatus convert(String code) {
			return CategoryStatus.fromCode(code);
		}
	}

	// 取代 Spring Boot 預設的 R2dbcCustomConversions，加入自訂的 Converter
	@Bean
	R2dbcCustomConversions r2dbcCustomConversions() {
		return R2dbcCustomConversions.of(PostgresDialect.INSTANCE,
				List.of(new CategoryStatusWritingConverter(), new CategoryStatusReadingConverter()));
	}
}
