package com.example.demo.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 存放在 Redis 的使用者（key：user:{id}），以 JSON 存放。
 * 使用 record：不可變，Jackson 3 可以直接用建構子反序列化，不需要 setter 或 Lombok。
 */
public record User(
		Long id,
		String name,
		String email,
		Integer age,
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updateTime) {
}
